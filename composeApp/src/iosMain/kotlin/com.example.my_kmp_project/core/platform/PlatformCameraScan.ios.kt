package com.example.my_kmp_project.core.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

private class IosCameraPermissionController : CameraPermissionController {
    private val statusState = mutableStateOf(readStatus())

    override val status: CameraPermissionStatus
        get() = statusState.value

    override fun refresh() {
        statusState.value = readStatus()
    }

    override fun requestPermission() {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> {
                statusState.value = CameraPermissionStatus.Granted
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        statusState.value =
                            if (granted) {
                                CameraPermissionStatus.Granted
                            } else {
                                CameraPermissionStatus.Denied
                            }
                    }
                }
            }
            else -> {
                statusState.value = CameraPermissionStatus.Denied
            }
        }
    }

    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    private fun readStatus(): CameraPermissionStatus =
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> CameraPermissionStatus.Granted
            AVAuthorizationStatusNotDetermined -> CameraPermissionStatus.Unknown
            AVAuthorizationStatusDenied,
            AVAuthorizationStatusRestricted,
            -> CameraPermissionStatus.Denied
            else -> CameraPermissionStatus.Denied
        }
}

@Composable
internal actual fun rememberCameraPermissionController(): CameraPermissionController {
    val controller = remember { IosCameraPermissionController() }
    LaunchedEffect(Unit) { controller.refresh() }
    return controller
}

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformBarcodeScanner(
    onBarcode: (payload: String) -> Unit,
    modifier: Modifier,
) {
    val session = remember { AVCaptureSession() }
    val handled = remember { mutableStateOf(false) }
    val delegate = remember {
        object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
            override fun captureOutput(
                captureOutput: platform.AVFoundation.AVCaptureOutput,
                didOutputMetadataObjects: List<*>,
                fromConnection: platform.AVFoundation.AVCaptureConnection,
            ) {
                if (handled.value) return
                val code = didOutputMetadataObjects
                    .filterIsInstance<AVMetadataMachineReadableCodeObject>()
                    .mapNotNull { it.stringValue }
                    .firstOrNull { it.isNotBlank() }
                if (code != null) {
                    handled.value = true
                    dispatch_async(dispatch_get_main_queue()) {
                        session.stopRunning()
                        onBarcode(code)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val input = device?.let {
            runCatching { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }.getOrNull()
        }
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
        }
        val output = AVCaptureMetadataOutput()
        if (session.canAddOutput(output)) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(delegate, dispatch_queue_create("scan.meta", null))
            output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
        }
        session.sessionPreset = AVCaptureSessionPresetHigh
        dispatch_async(dispatch_queue_create("scan.session", null)) {
            session.startRunning()
        }
        onDispose {
            session.stopRunning()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        UIKitView(
            factory = {
                val view = UIView(frame = CGRectZero.readValue())
                view.backgroundColor = UIColor.blackColor
                val previewLayer = AVCaptureVideoPreviewLayer(session = session)
                previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                view.layer.addSublayer(previewLayer)
                view
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val preview = view.layer.sublayers
                    ?.mapNotNull { it as? AVCaptureVideoPreviewLayer }
                    ?.firstOrNull()
                if (preview != null) {
                    preview.frame = view.bounds
                }
            },
        )
        Text(
            text = "将二维码放入框内",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }
}
