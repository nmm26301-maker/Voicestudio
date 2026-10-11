#!/usr/bin/env python3
"""Prepare the manual build manifest for original, preview or standalone app."""
import argparse
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("source")
parser.add_argument("destination")
mode = parser.add_mutually_exclusive_group()
mode.add_argument("--preview", action="store_true")
mode.add_argument("--standalone", action="store_true")
args = parser.parse_args()
android = "{http://schemas.android.com/apk/res/android}"
ET.register_namespace("android", "http://schemas.android.com/apk/res/android")
tree = ET.parse(args.source)
root = tree.getroot()
namespace = root.get("package")
for parent in root.iter():
    for child in list(parent):
        if child.get("{http://schemas.android.com/tools}node") == "remove":
            parent.remove(child)
if args.preview or args.standalone:
    # Keep Java class names in their original namespace; only the installation
    # identity and provider authority change. Relative names must become full.
    package = namespace + ".preview"
    for component in root.iter():
        if component.tag in ("application", "activity", "service", "provider", "receiver"):
            name = component.get(android + "name", "")
            if name.startswith("."):
                component.set(android + "name", namespace + name)
            elif name and "." not in name:
                component.set(android + "name", namespace + "." + name)
        if component.tag == "provider":
            authority = component.get(android + "authorities", "")
            if authority == namespace + ".audio":
                component.set(android + "authorities", package + ".audio")
    root.set("package", package)
    if args.preview:
        root.set(android + "versionName", root.get(android + "versionName") + "-preview")
        root.find("application").set(android + "label", "VoiceStudio 测试版")
tree.write(args.destination, encoding="utf-8", xml_declaration=True)
