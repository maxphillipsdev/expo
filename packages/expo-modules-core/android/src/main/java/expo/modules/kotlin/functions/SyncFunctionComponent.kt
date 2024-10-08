package expo.modules.kotlin.functions

import com.facebook.react.bridge.ReadableArray
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.exception.FunctionCallException
import expo.modules.kotlin.exception.exceptionDecorator
import expo.modules.kotlin.jni.JNIFunctionBody
import expo.modules.kotlin.jni.decorators.JSDecoratorsBridgingObject
import expo.modules.kotlin.types.AnyType
import expo.modules.kotlin.types.JSTypeConverter

class SyncFunctionComponent(
  name: String,
  desiredArgsTypes: Array<AnyType>,
  private val body: (args: Array<out Any?>) -> Any?
) : AnyFunction(name, desiredArgsTypes) {
  private var shouldUseExperimentalConverter = false

  @Suppress("FunctionName")
  fun UseExperimentalConverter(shouldUse: Boolean = true) = apply {
    shouldUseExperimentalConverter = shouldUse
  }

  @Throws(CodedException::class)
  fun call(args: ReadableArray): Any? {
    return body(convertArgs(args))
  }

  fun call(args: Array<Any?>, appContext: AppContext? = null): Any? {
    return body(convertArgs(args, appContext))
  }

  internal fun getJNIFunctionBody(moduleName: String, appContext: AppContext?): JNIFunctionBody {
    return JNIFunctionBody { args ->
      return@JNIFunctionBody exceptionDecorator({
        FunctionCallException(name, moduleName, it)
      }) {
        val result = call(args, appContext)
        return@exceptionDecorator JSTypeConverter.convertToJSValue(result, useExperimentalConverter = shouldUseExperimentalConverter)
      }
    }
  }

  override fun attachToJSObject(appContext: AppContext, jsObject: JSDecoratorsBridgingObject, moduleName: String) {
    jsObject.registerSyncFunction(
      name,
      takesOwner,
      getCppRequiredTypes().toTypedArray(),
      getJNIFunctionBody(moduleName, appContext)
    )
  }
}
