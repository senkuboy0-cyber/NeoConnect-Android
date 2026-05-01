package com.neoconnect.app.webrtc

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.os.Build
import android.util.Log
import org.json.JSONObject
import org.webrtc.*

class CallManager(private val context: Context) {

    private var socket: io.socket.client.Socket? = null
    private var peerConnection: PeerConnection? = null
    private var factory: PeerConnectionFactory? = null
    private var localStream: MediaStream? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var otherUserId: String? = null
    
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    var eglBase: EglBase? = null
    private var savedAudioMode = 0

    var onRemoteStream: ((VideoTrack) -> Unit)? = null
    var onCallEnded: (() -> Unit)? = null
    var onWaiting: (() -> Unit)? = null
    var onConnected: (() -> Unit)? = null

    private val SERVER_URL = "https://call-signaling-server.onrender.com"

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    fun init() {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        savedAudioMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        
        if (eglBase == null) {
            eglBase = EglBase.create()
        }
        
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase?.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase?.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    @SuppressLint("MissingPermission")
    fun startLocalVideoCapture(localView: SurfaceViewRenderer) {
        val ctx = eglBase?.eglBaseContext ?: return
        val videoSource = factory?.createVideoSource(false)
        videoCapturer = createCameraCapturer()
        val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", ctx)
        videoCapturer?.initialize(surfaceHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)
        localVideoTrack = factory?.createVideoTrack("video0", videoSource)
        localVideoTrack?.addSink(localView)
        localStream?.addTrack(localVideoTrack)
    }
    
    fun startLocalAudio() {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googDAEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))
        }
        val audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack("audio0", audioSource)
        localStream?.addTrack(localAudioTrack)
    }

    fun createStream(isVideoCall: Boolean) {
        localStream = factory?.createLocalMediaStream("stream0")
        startLocalAudio()
        setupAudio(isVideoCall)
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        return enumerator.deviceNames
            .firstOrNull { enumerator.isFrontFacing(it) }
            ?.let { enumerator.createCapturer(it, null) }
    }

    fun joinRoom(roomId: String) {
        socket = io.socket.client.IO.socket(SERVER_URL)
        socket?.connect()

        socket?.on(io.socket.client.Socket.EVENT_CONNECT) {
            socket?.emit("join-room", roomId)
        }

        socket?.on("waiting") { onWaiting?.invoke() }

        socket?.on("ready") { args ->
            val data = args[0] as JSONObject
            otherUserId = data.getString("otherUserId")
            val shouldCreateOffer = data.getBoolean("shouldCreateOffer")
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            createPeerConnection()
            if (shouldCreateOffer) createOffer()
            onConnected?.invoke()
        }

        socket?.on("offer") { args ->
            val data = args[0] as JSONObject
            otherUserId = data.getString("from")
            val offer = data.getJSONObject("offer")
            createPeerConnection()
            peerConnection?.setRemoteDescription(
                SimpleSdpObserver(),
                SessionDescription(SessionDescription.Type.OFFER, offer.getString("sdp"))
            )
            createAnswer()
        }

        socket?.on("answer") { args ->
            val data = args[0] as JSONObject
            val answer = data.getJSONObject("answer")
            peerConnection?.setRemoteDescription(
                SimpleSdpObserver(),
                SessionDescription(SessionDescription.Type.ANSWER, answer.getString("sdp"))
            )
        }

        socket?.on("ice-candidate") { args ->
            val data = args[0] as JSONObject
            val candidate = data.getJSONObject("candidate")
            peerConnection?.addIceCandidate(
                IceCandidate(
                    candidate.getString("sdpMid"),
                    candidate.getInt("sdpMLineIndex"),
                    candidate.getString("candidate")
                )
            )
        }

        socket?.on("end-call") { endCall() }
        socket?.on(io.socket.client.Socket.EVENT_DISCONNECT) { onCallEnded?.invoke() }
    }

    private fun createPeerConnection() {
        val config = PeerConnection.RTCConfiguration(iceServers)
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        
        peerConnection = factory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val json = JSONObject()
                json.put("to", otherUserId)
                val c = JSONObject()
                c.put("sdpMid", candidate.sdpMid)
                c.put("sdpMLineIndex", candidate.sdpMLineIndex)
                c.put("candidate", candidate.sdp)
                json.put("candidate", c)
                socket?.emit("ice-candidate", json)
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<MediaStream>?) {
                val track = receiver?.track() ?: return
                if (track is VideoTrack) {
                    track.setEnabled(true)
                    onRemoteStream?.invoke(track)
                }
            }
            
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.DISCONNECTED || state == PeerConnection.IceConnectionState.CLOSED) {
                    onCallEnded?.invoke()
                }
            }
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        })

        localStream?.let { stream ->
            stream.audioTracks.forEach { peerConnection?.addTrack(it) }
            stream.videoTracks.forEach { peerConnection?.addTrack(it) }
        }
    }

    private fun createOffer() {
        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(SimpleSdpObserver(), it)
                    val json = JSONObject()
                    json.put("to", otherUserId)
                    val offer = JSONObject()
                    offer.put("type", it.type.canonicalForm())
                    offer.put("sdp", it.description)
                    json.put("offer", offer)
                    socket?.emit("offer", json)
                }
            }
        }, MediaConstraints())
    }

    private fun createAnswer() {
        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(SimpleSdpObserver(), it)
                    val json = JSONObject()
                    json.put("to", otherUserId)
                    val answer = JSONObject()
                    answer.put("type", it.type.canonicalForm())
                    answer.put("sdp", it.description)
                    json.put("answer", answer)
                    socket?.emit("answer", json)
                }
            }
        }, MediaConstraints())
    }

    fun toggleMute(mute: Boolean) { localAudioTrack?.setEnabled(!mute) }
    fun toggleCamera(off: Boolean) { localVideoTrack?.setEnabled(!off) }
    fun switchCamera() { (videoCapturer as? CameraVideoCapturer)?.switchCamera(null) }
    
    private fun setupAudio(isVideoCall: Boolean) {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices
            val speakerDevice = devices?.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            val earDevice = devices?.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
            if (isVideoCall) {
                speakerDevice?.let { audioManager?.setCommunicationDevice(it) }
            } else {
                earDevice?.let { audioManager?.setCommunicationDevice(it) }
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = isVideoCall
        }
        setAudioFocus(true)
    }

    fun enableSpeaker(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices
            val device = if (enable) {
                devices?.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            } else {
                devices?.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
            }
            device?.let { audioManager?.setCommunicationDevice(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = enable
        }
    }

    private fun setAudioFocus(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (enable) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build())
                    .build()
                audioManager?.requestAudioFocus(audioFocusRequest!!)
            } else { audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) } }
        } else {
            @Suppress("DEPRECATION")
            if (enable) audioManager?.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN)
            else audioManager?.abandonAudioFocus(null)
        }
    }

    fun endCall() {
        setAudioFocus(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager?.clearCommunicationDevice()
        }
        audioManager?.mode = savedAudioMode
        @Suppress("DEPRECATION")
        audioManager?.isSpeakerphoneOn = false
        
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        peerConnection?.close()
        socket?.disconnect()
        onCallEnded?.invoke()
    }
}

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) { Log.e("SDP", "Error: $error") }
    override fun onSetFailure(error: String?) { Log.e("SDP", "Error: $error") }
}
