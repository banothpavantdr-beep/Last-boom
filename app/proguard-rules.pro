# Add project specific ProGuard rules here.
# Keep the Device Admin receiver so reflection-based policy callbacks always resolve.
-keep class com.jarvis.assistant.JarvisDeviceAdminReceiver { *; }
-keep class com.jarvis.assistant.JarvisOverlayService { *; }
