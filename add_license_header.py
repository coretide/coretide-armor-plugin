#!/usr/bin/env python3
import os
from pathlib import Path

LICENSE_HEADER = '''/*
 * Copyright 2025 Kushal Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
'''

def has_license_header(lines):
    return any("Licensed under the Apache License" in line for line in lines)

def prepend_license_to_file(file_path):
    try:
        # Try UTF-8 first
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        if not has_license_header(lines):
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(LICENSE_HEADER + "\n" + "".join(lines))
            print(f"✅ Added license header to: {file_path}")
        else:
            print(f"⏭️  Already has license header: {file_path}")

    except UnicodeDecodeError:
        try:
            # Fallback to latin-1 if UTF-8 fails
            with open(file_path, 'r', encoding='latin-1') as f:
                lines = f.readlines()

            if not has_license_header(lines):
                with open(file_path, 'w', encoding='latin-1') as f:
                    f.write(LICENSE_HEADER + "\n" + "".join(lines))
                print(f"✅ Added license header to: {file_path} (latin-1)")
            else:
                print(f"⏭️  Already has license header: {file_path}")

        except Exception as e:
            print(f"❌ Error processing {file_path}: {e}")
    except Exception as e:
        print(f"❌ Error processing {file_path}: {e}")

def main():
    print("🛡️ Adding license headers to source files...")

    file_count = 0
    processed_count = 0

    for ext in ['.java', '.kt', '.groovy']:
        for path in Path("src").rglob(f'*{ext}'):
            file_count += 1
            prepend_license_to_file(path)
            processed_count += 1

    print(f"\n📊 Summary:")
    print(f"  • Files found: {file_count}")
    print(f"  • Files processed: {processed_count}")
    print(f"✅ License header processing completed!")

if __name__ == "__main__":
    main()
