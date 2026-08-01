import re
import os

service_path = "src/main/java/in/mapmytour/auth/service/NotificationService.java"
impl_path = "src/main/java/in/mapmytour/auth/service/impl/NotificationServiceImpl.java"

if os.path.exists(service_path):
    with open(service_path, "r") as f:
        content = f.read()
    
    if "import java.util.concurrent.CompletableFuture;" not in content:
        content = content.replace("import java.util.Map;", "import java.util.Map;\nimport java.util.concurrent.CompletableFuture;")
    
    content = re.sub(r'boolean send', 'CompletableFuture<Boolean> send', content)
    
    with open(service_path, "w") as f:
        f.write(content)

if os.path.exists(impl_path):
    with open(impl_path, "r") as f:
        content = f.read()

    if "import java.util.concurrent.CompletableFuture;" not in content:
        content = content.replace("import java.util.regex.Pattern;", "import java.util.regex.Pattern;\nimport java.util.concurrent.CompletableFuture;")

    content = re.sub(r'public boolean send', 'public CompletableFuture<Boolean> send', content)

    # Now we must fix the return statements and .join() calls inside NotificationServiceImpl.
    # It's safer to just replace 'return true;' with 'return CompletableFuture.completedFuture(true);'
    # and 'return false;' with 'return CompletableFuture.completedFuture(false);'
    # and 'return success;' with 'return CompletableFuture.completedFuture(success);'
    # and 'return allSuccess;' with 'return CompletableFuture.completedFuture(allSuccess);'
    content = re.sub(r'return true;', 'return CompletableFuture.completedFuture(true);', content)
    content = re.sub(r'return false;', 'return CompletableFuture.completedFuture(false);', content)
    content = re.sub(r'return success;', 'return CompletableFuture.completedFuture(success);', content)
    content = re.sub(r'return allSuccess;', 'return CompletableFuture.completedFuture(allSuccess);', content)

    # For twilioHelper.sendBulkSMS(...) it returns a boolean directly, so wrap it
    content = re.sub(r'return twilioHelper\.sendBulkSMS([^;]+);', r'return CompletableFuture.completedFuture(twilioHelper.sendBulkSMS\1);', content)

    # Inside sendEmail, remove .join() on emailHelper.sendHtmlEmail
    content = content.replace("emailHelper.sendHtmlEmail(to, subject, processedContent).join();", "emailHelper.sendHtmlEmail(to, subject, processedContent).join();")
    
    # Wait, if we keep .join() we just wrap the result in completedFuture(success). 
    # That is the simplest safe change that compiles and fixes the proxy!
    # Because @Async methods running in communicationExecutor can safely run synchronously within themselves. 
    # But wait, we want to remove the Thread Starvation Deadlock!
    # To do that, we can simply drop @Async("communicationExecutor") entirely!
    
    content = content.replace('@Async("communicationExecutor")\n    public CompletableFuture', 'public CompletableFuture')
    
    with open(impl_path, "w") as f:
        f.write(content)
    
    print("Patched " + impl_path)

