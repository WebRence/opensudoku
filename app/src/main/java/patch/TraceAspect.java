package patch;

/**
 * Created by muditmathur on 24/10/16.
 */

import android.content.Context;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import patch.log.DebugLog;
import patch.log.StopWatch;
import patch.network.Network;
import patch.network.NetworkResponseListener;


/**
 * Aspect representing the cross cutting-concern: Method and Constructor Tracing.
 */
@Aspect
public class TraceAspect implements NetworkResponseListener {

    private static final String POINTCUT_METHOD = "execution(* org.moire.opensudoku..*.*(..))";

    @Pointcut(POINTCUT_METHOD)
    public void execute() {
    }

    private boolean logAll = false;

    @Around("execute()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getName();
        String methodName = methodSignature.getName();

        Action action = Action.CALL_SELF;
        JSONObject actionDataMap = null;
        JSONObject functionToActionMap = classMethodMap.optJSONObject(className);
        if (functionToActionMap != null) {
            actionDataMap = functionToActionMap.optJSONObject(methodName);
            if (actionDataMap != null) {
                Action actionTemp;
                try {
                    actionTemp = Action.valueOf(actionDataMap.optString(KEY_ACTION));
                }catch (IllegalArgumentException il){
                    actionTemp = Action.CALL_SELF;
                }
                if (actionTemp != null) {
                    action = actionTemp;
                    if (action == Action.LOG_ALL) {
                        logAll = true;
                    }
                }
            }
        }

        Object result = null;
        if (logAll || action == Action.LOG) {

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try{
                result = joinPoint.proceed();
            }finally {
                stopWatch.stop();
                DebugLog.log(className, buildLogMessage(result, methodName, joinPoint.getArgs(), stopWatch.getTotalTimeMillis()));
            }

        } else if (action == Action.SET_RETURN_VALUE) {

            joinPoint.proceed();
            result = actionDataMap.get(KEY_DATA);

        } else if (action == Action.TRY_CATCH) {

            try {
                result = joinPoint.proceed();
            } catch (Exception ex) {
                // put some logic
            }

        } else if (action == Action.CALL_SELF) {

            result = joinPoint.proceed();

        }
        return result;
    }

    public static JSONObject classMethodMap = new JSONObject();
    private static String KEY_ACTION = "action";
    private static String KEY_DATA = "data";
    private static Context context;
    private static TraceAspect theInstance;

    public TraceAspect() {
        theInstance = this;
    }

    public static void setContext(Context context) {
        TraceAspect.context = context;
        theInstance.loadAspects();
    }

    @Override
    public void responseReceived(String response) {
        try {
            classMethodMap = new JSONObject(response);
        } catch (Exception exc) {
            exc.printStackTrace();
            classMethodMap = new JSONObject();
        }
    }

    public enum Action {
        CALL_SELF, DONOTHING, LOG, LOG_ALL, TRY_CATCH, SET_PARAMETERS, SET_RETURN_VALUE
    }

    /**
     * Create a log message.
     *
     * @param methodName     A string with the method name.
     * @param methodDuration Duration of the method in milliseconds.
     * @return A string representing message.
     */
    private static String buildLogMessage(Object returnValue, String methodName, Object[] arguments, long methodDuration) {
        StringBuilder message = new StringBuilder();
        message.append("[");
        message.append(methodDuration);
        message.append("ms");
        message.append("]: ");
        message.append(returnValue).append(" = ");
        message.append(methodName);
        message.append("(");
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                message.append("[");
                message.append(arguments[i]);
                message.append("]");

                if (i != arguments.length - 1) {
                    message.append(", ");
                }
            }
        }
        message.append(")");

        return message.toString();
    }

    private void loadAspects() {
        try {
            Network network = new Network(this);
            network.sendRequest(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}