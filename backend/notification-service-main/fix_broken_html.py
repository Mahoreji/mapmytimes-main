import os

templates_dir = "internal/app/email/templates"

broken_snippet = """    <!-- BRAND LOGO PILL -->
    <div style="display:inline-block; background:#ffffff; padding:14px 28px; border-radius:999px; box-shadow:0 10px 25px rgba(0,0,0,0.2); margin-bottom:34px;">
"""

for filename in os.listdir(templates_dir):
    if filename.endswith(".html"):
        file_path = os.path.join(templates_dir, filename)
        with open(file_path, "r") as f:
            content = f.read()

        if broken_snippet in content:
            content = content.replace(broken_snippet, "")
            with open(file_path, "w") as f:
                f.write(content)
            print(f"Fixed broken HTML in {filename}")

print("All broken templates fixed.")
