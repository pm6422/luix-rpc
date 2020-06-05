//package org.infinity.rpc.core.exception;
//
//public abstract class MotanAbstractException extends RuntimeException {
//    private static final long serialVersionUID = -8742311167276890503L;
//
//    protected MotanErrorMsg motanErrorMsg = MotanErrorMsgConstant.FRAMEWORK_DEFAULT_ERROR;
//    protected String errorMsg = null;
//
//    public MotanAbstractException() {
//        super();
//    }
//
//    public MotanAbstractException(MotanErrorMsg motanErrorMsg) {
//        super();
//        this.motanErrorMsg = motanErrorMsg;
//    }
//
//    public MotanAbstractException(String message) {
//        super(message);
//        this.errorMsg = message;
//    }
//
//    public MotanAbstractException(String message, MotanErrorMsg motanErrorMsg) {
//        super(message);
//        this.motanErrorMsg = motanErrorMsg;
//        this.errorMsg = message;
//    }
//
//    public MotanAbstractException(String message, Throwable cause) {
//        super(message, cause);
//        this.errorMsg = message;
//    }
//
//    public MotanAbstractException(String message, Throwable cause, MotanErrorMsg motanErrorMsg) {
//        super(message, cause);
//        this.motanErrorMsg = motanErrorMsg;
//        this.errorMsg = message;
//    }
//
//    public MotanAbstractException(Throwable cause) {
//        super(cause);
//    }
//
//    public MotanAbstractException(Throwable cause, MotanErrorMsg motanErrorMsg) {
//        super(cause);
//        this.motanErrorMsg = motanErrorMsg;
//    }
//
//    @Override
//    public String getMessage() {
//        String message = getOriginMessage();
//
//        return "error_message: " + message + ", status: " + motanErrorMsg.getStatus() + ", error_code: " + motanErrorMsg.getErrorCode()
//                + ",r=" + RpcContext.getContext().getRequestId();
//    }
//
//    public String getOriginMessage(){
//        if (motanErrorMsg == null) {
//            return super.getMessage();
//        }
//
//        String message;
//
//        if (errorMsg != null && !"".equals(errorMsg)) {
//            message = errorMsg;
//        } else {
//            message = motanErrorMsg.getMessage();
//        }
//        return message;
//    }
//
//    public int getStatus() {
//        return motanErrorMsg != null ? motanErrorMsg.getStatus() : 0;
//    }
//
//    public int getErrorCode() {
//        return motanErrorMsg != null ? motanErrorMsg.getErrorCode() : 0;
//    }
//
//    public MotanErrorMsg getMotanErrorMsg() {
//        return motanErrorMsg;
//    }
//}
