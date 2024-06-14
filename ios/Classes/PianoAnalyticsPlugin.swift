import Flutter
import UIKit
import PianoAnalytics

public class PianoAnalyticsPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "piano_analytics_plugin", binaryMessenger: registrar.messenger())
    let instance = PianoAnalyticsPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch (call.method) {
        case "setConfiguration":
            guard   let args = call.arguments as? Dictionary<String, AnyObject>,
                    let collectDomain = args["collectDomain"] as? String,
                    let site = args["site"] as? Int,
                    let privacyDefaultMode = args["privacyDefaultMode"] as? String?
            else{
                result(FlutterError(code: "500", message: "Error setConfiguration :: collectDomain and site parameters is required", details: nil))
                return
            }
        
                
            if let privacyDefaultMode = privacyDefaultMode {
                pa.setConfiguration(ConfigurationBuilder()
                    .withCollectDomain(collectDomain)
                    .withSite(site)
                    .withPrivacyDefaultMode(privacyDefaultMode)
                    .build()
                )
                
                pa.privacyIncludeEvent("publisher.impression")
                pa.privacyIncludeEvent("publisher.click")
                pa.privacyIncludeProperty("customobject")
                pa.privacyIncludeProperty("customobject_certif_device")
                pa.privacyIncludeProperty("customobject_certif_platform")
                pa.privacyIncludeProperty("customobject_device")
                
                return
            }

            pa.setConfiguration(ConfigurationBuilder()
                .withCollectDomain(collectDomain)
                .withSite(site)
                .build()
            )
        
        break
        case "sendEvent":
            guard   let args = call.arguments as? Dictionary<String, AnyObject>,
                    let eventName = args["eventName"] as? String
            else{
                result(FlutterError(code: "500", message: "Error sendEvent :: eventName is required", details: nil))
                return
            }
        
        
        if let data = args["data"] as? Dictionary<String, AnyObject> {
            pa.sendEvent(Event(eventName, data: data))
        } else {
            pa.sendEvent(Event(eventName, data: [:]))
        }

        break
        default:
            result(FlutterMethodNotImplemented)
    }
  }
}
