package com.neoconnect.app.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import io.socket.client.IO
import io.socket.client.Socket
import org.webrtc.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CallManager {
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var socket: Socket? = null
    private var otherUserId: String = ""
    private var eglBase: EglBase? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    var onRemoteStream: ((VideoTrack) -> Unit)? = null
    var onCallEnded: (() -> Unit)? = null

    fun init(surfaceView: SurfaceView, context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        eglBase = EglBase.create()
        surfaceView.holder?.setFixatedSize(1920, 1080)
        surfaceView.setEnableHardwareAcceleration(true)
        
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        
        val options = PeerConnectionFactory.Options()
        val encoderFactory = DefaultVideoEncoderFactory(eglBase?.eglContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglContext)
        
        peerConnection = PeerConnectionFactory(options).createPeerConnection(
            makePeerConstraints(),
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    val json = JSONObject()
                    json.put("to", otherUserId)
                    json.put("label", candidate?.sdpMLineIndex)
                    json.put("candidate", candidate?.sdp)
                    socket?.emit("candidate", json)
                }
                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    if (track is VideoTrack) {
                        onRemoteStream?.invoke(track)
                        track.addSink(surfaceView)
                    }
                }
            }
        )
        
        startLocalVideoTrack(context, surfaceView)
    }

    fun initLocal(surfaceView: SurfaceView) {
        localVideoTrack?.addSink(surfaceView)
    }

    private fun startLocalVideoTrack(context: Context, surfaceView: SurfaceView) {
        val videoSource = PeerConnectionFactory.createVideoSource(
            Camera2Capturer(context as android.app.Activity, null)
        )
        videoCapturer = Camera2Capturer(context as android.app.Activity, null)
        localVideoTrack = PeerConnectionFactory.createVideoTrack("video", videoSource)
        
        val audioSource = PeerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = PeerConnectionFactory.createAudioTrack("audio", audioSource)
        
        localVideoTrack?.addSink(surfaceView)
    }

    private fun makePeerConstraints(): PeerConnection.RTCConfiguration {
        return PeerConnection.RTCConfiguration(
            arrayOf(
                IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
    }

    fun joinRoom(roomId: String) {
        try {
            socket = IO.socket("http://localhost:3000").apply {
                connect()
                on("user.connected") { args ->
                    otherUserId = args[0].toString()
                    createOffer()
                }
                on("offer") { args ->
                    val offerJson = args[0] as JSONObject
                    val sdp = SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(offerJson.getString("type")),
                        offerJson.getString("sdp")
                    )
                    peerConnection?.setRemoteDescription(SimpleSdpObserver(), sdp)
                    createAnswer()
                }
                on("answer") { args ->
                    val answerJson = args[0] as JSONObject
                    val sdp = SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(answerJson.getString("type")),
                        answerJson.getString("sdp")
                    )
                    peerConnection?.setRemoteDescription(SimpleSdpObserver(), sdp)
                }
                on("candidate") { args ->
                    val candidateJson = args[0] as JSONObject
                    val candidate = IceCandidate.builder()
                        .sdpMid(candidateJson.optString("id", "0"))
                        .sdpLineIndex(candidateJson.getInt("label"))
                        .candidate(candidateJson.getString("candidate"))
                        .build()
                    peerConnection?.addIceCandidate(candidate)
                }
                on("call.left") {
                    onCallEnded?.invoke()
                }
            }
            socket?.emit("join", roomId)
            setAudioFocus(true)
        } catch (e: Exception) {
            Log.e("CallManager", "Error joining room", e)
        }
    }

    private fun createOffer() {
        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                val json = JSONObject()
                json.put("to", otherUserId)
                val offer = JSONObject()
                offer.put("type", sdp.type.canonicalForm())
                offer.put("sdp", sdp.description)
                json.put("offer", offer)
                socket?.emit("offer", json)
            }
        }, MediaConstraints())
    }

    private fun createAnswer() {
        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                val json = JSONObject()
                json.put("to", otherUserId)
                val answer = JSONObject()
                answer.put("type", sdp.type.canonicalForm())
                answer.put("sdp", sdp.description)
                json.put("answer", answer)
                socket?.emit("answer", json)
            }
        }, MediaConstraints())
    }

    fun toggleMute(mute: Boolean) {
        localAudioTrack?.setEnabled(!mute)
    }

    fun toggleCamera(off: Boolean) {
        localVideoTrack?.setEnabled(!off)
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    private fun setAudioFocus(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (enable) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build())
                    .build()
                audioManager?.requestAudioFocus(audioFocusRequest!!)
            } else {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            }
        } else {
            @Suppress("DEPRECATION")
            if (enable) audioManager?.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN)
            else audioManager?.abandonAudioFocus(null)
        }
    }

    fun endCall() {
        setAudioFocus(false)
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
