package com.plugin.example.piano_analytics_plugin

import android.content.Context
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.piano.android.analytics.Configuration
import io.piano.android.analytics.PianoAnalytics
import io.piano.android.analytics.model.Event
import io.piano.android.analytics.model.PrivacyMode
import io.piano.android.analytics.model.Property
import io.piano.android.analytics.model.PropertyName
import io.piano.android.consents.PianoConsents
import io.piano.android.consents.models.ConsentConfiguration
import io.piano.android.consents.models.ConsentMode

object PAEvents {
    const val SET_CONFIGURATION = "setConfiguration"
    const val SEND_EVENT = "sendEvent"
}

/** PianoAnalyticsPlugin */
class PianoAnalyticsPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context
    private lateinit var pianoAnalytics: PianoAnalytics

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "piano_analytics_plugin")
        channel.setMethodCallHandler(this)
        applicationContext = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        try {

            val arguments: HashMap<String, Any> = call.arguments as HashMap<String, Any>

            when (call.method) {
                PAEvents.SET_CONFIGURATION -> {
                    val collectDomain: String = arguments["collectDomain"] as String
                    val site: Int = arguments["site"] as Int
                    val privacyDefaultMode: String? = arguments["privacyDefaultMode"] as String?

                    if (collectDomain.isEmpty()) {
                        result.error("500", "collectDomain is required", null)
                        return
                    }

                    if (privacyDefaultMode != null) {

                        println("[PIANO_PLUGIN] creating configuration")
                        val configuration = Configuration.Builder(
                            collectDomain = collectDomain,
                            site = site,
                            defaultPrivacyMode = PrivacyMode.EXEMPT
                        ).build()

                        println("[PIANO_PLUGIN] initializing pianoConsents")
                        val pianoConsents = PianoConsents.init(
                            applicationContext,
                            consentConfiguration = ConsentConfiguration(
                                requireConsent = false,
                            )
                        )

                        //pianoConsents.setAll(ConsentMode.ESSENTIAL)

                        println("[PIANO_PLUGIN] setting privacyDefaultMode event names")

                        PrivacyMode.EXEMPT.allowedEventNames.addAll(
                            arrayOf(
                                Event.PAGE_DISPLAY,
                                Event.PUBLISHER_IMPRESSION,
                                Event.PUBLISHER_CLICK,
                            )
                        )

                        println("[PIANO_PLUGIN] setting customobject property keys")

                        PrivacyMode.EXEMPT.allowedPropertyKeys["customobject"]?.addAll(
                            arrayOf(
                                PropertyName("customobject"),
                                PropertyName("customobject_certif_device"),
                                PropertyName("customobject_certif_platform"),
                                PropertyName("customobject_device")
                            )
                        )

                        println("[PIANO_PLUGIN] setting publisher impression property keys")
                        PrivacyMode.EXEMPT.allowedPropertyKeys[Event.PUBLISHER_IMPRESSION]?.addAll(
                            arrayOf(
                                PropertyName("onsitead_type"),
                                PropertyName("onsitead_campaign"),
                                PropertyName("onsitead_format"),
                                PropertyName("onsitead_advertiser"),
                                PropertyName("onsitead_url"),
                                PropertyName("onsitead_creation")
                            )
                        )

                        println("[PIANO_PLUGIN] setting publisher click property keys")
                        PrivacyMode.EXEMPT.allowedPropertyKeys[Event.PUBLISHER_CLICK]?.addAll(
                            arrayOf(
                                PropertyName("onsitead_type"),
                                PropertyName("onsitead_campaign"),
                                PropertyName("onsitead_format"),
                                PropertyName("onsitead_advertiser"),
                                PropertyName("onsitead_url"),
                                PropertyName("onsitead_creation")
                            )
                        )

                        println("[PIANO_PLUGIN] initializing")
                        pianoAnalytics = PianoAnalytics.init(
                            applicationContext,
                            configuration,
                            pianoConsents
                        )

                        return
                    }
                    println("privacyDefaultMode is null")
                }

                PAEvents.SEND_EVENT -> {
                    val eventName: String = arguments["eventName"] as String

                    if (eventName.isEmpty()) {
                        result.error("500", "eventName is required", null)
                    } else {
                        val data: HashMap<String, Any?> = arguments["data"] as HashMap<String, Any?>

                        println("[PIANO_PLUGIN] sending event")

                        val properties = data.map { (key, value) ->
                            Property(PropertyName(key), value.toString())
                        }.toMutableSet()

                        println("[PIANO_PLUGIN] sending event with properties: $properties")

                        pianoAnalytics.sendEvents(
                            Event.Builder(
                                name = eventName,
                                properties = properties,
                            ).build()
                        )

                        println("[PIANO_PLUGIN] event sent")
                        result.success(null)
                    }
                }

                else -> result.notImplemented()
            }
        } catch (e: Exception) {
            result.error("500", e.toString(), null)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
