import re
import os

impl_path = "src/main/java/in/mapmytour/auth/service/impl/NotificationServiceImpl.java"

if os.path.exists(impl_path):
    with open(impl_path, "r") as f:
        content = f.read()

    # In sendBulkEmail:
    content = content.replace("!sendEmail(recipient, subject, content, templateName, variables)",
                              "!sendEmail(recipient, subject, content, templateName, variables).join()")
    content = content.replace("!sendPushNotification(deviceToken, title, message, data)",
                              "!sendPushNotification(deviceToken, title, message, data).join()")
    content = content.replace("!sendNotificationToUser(userEmail, request)",
                              "!sendNotificationToUser(userEmail, request).join()")
                              
    # In sendNotificationToUser:
    content = content.replace("success = sendEmail(userEmail, request.getTitle(), request.getMessage(),\n                                request.getTemplateName(), request.getTemplateVariables());",
                              "success = sendEmail(userEmail, request.getTitle(), request.getMessage(),\n                                request.getTemplateName(), request.getTemplateVariables()).join();")
    content = content.replace("success = sendSMS(user.getPhone(), request.getMessage());",
                              "success = sendSMS(user.getPhone(), request.getMessage()).join();")
    content = content.replace('success = sendPushNotification("device_token_placeholder", request.getTitle(),\n                                request.getMessage(), request.getMetadata());',
                              'success = sendPushNotification("device_token_placeholder", request.getTitle(),\n                                request.getMessage(), request.getMetadata()).join();')

    content = content.replace("emailSuccess = sendEmail(userEmail, request.getTitle(), request.getMessage(),\n                                request.getTemplateName(), request.getTemplateVariables());",
                              "emailSuccess = sendEmail(userEmail, request.getTitle(), request.getMessage(),\n                                request.getTemplateName(), request.getTemplateVariables()).join();")
    content = content.replace("smsSuccess = sendSMS(user.getPhone(), request.getMessage());",
                              "smsSuccess = sendSMS(user.getPhone(), request.getMessage()).join();")
    content = content.replace('pushSuccess = sendPushNotification("device_token_placeholder", request.getTitle(),\n                                request.getMessage(), request.getMetadata());',
                              'pushSuccess = sendPushNotification("device_token_placeholder", request.getTitle(),\n                                request.getMessage(), request.getMetadata()).join();')

    # Return wrappers where it calls another send method
    content = re.sub(r'return CompletableFuture\.completedFuture\(sendEmail\((.*?)\)\);',
                     r'return sendEmail(\1);', content, flags=re.DOTALL)
    
    # We must be careful because methods like sendWelcomeNotification do:
    # return CompletableFuture.completedFuture(sendEmail(user.getEmail(), ...));
    # but sendEmail returns CompletableFuture<Boolean>.
    # By changing it to return sendEmail(...), we fix the type mismatch (CompletableFuture<CompletableFuture<Boolean>> instead of CompletableFuture<Boolean>).
    # Wait, earlier I did: `content = re.sub(r'return success;', ...)` which doesn't wrap the method calls directly unless it was `return sendEmail(...)`.
    # Let's find any `return CompletableFuture.completedFuture(sendEmail(` and replace it with `return sendEmail(`.
    content = content.replace("return CompletableFuture.completedFuture(sendEmail(", "return sendEmail(")
    content = content.replace("return CompletableFuture.completedFuture(sendSMS(", "return sendSMS(")
    content = content.replace("return CompletableFuture.completedFuture(sendNotificationToUsers(", "return sendNotificationToUsers(")

    with open(impl_path, "w") as f:
        f.write(content)
    
    print("Fixed compilation issues")

