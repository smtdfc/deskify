# Deskify ProGuard / R8 rules.
#
# The app uses reflection in exactly two places:
#   1. OEM desktop-mode component probing (Samsung DeX, etc.) - the
#      class names are *probe targets*, not entry points, so R8
#      does not need to keep them; a missing class is a valid answer.
#   2. The DataStore serialization of the desktop layout - the
#      @Serializable classes are kept by the Kotlin plugin's
#      auto-generated metadata.
#
# We do not need any -keep rules in this stage. If a later stage
# adds a JNI bridge or a Shizuku binder, the rules will be added
# there with a comment explaining why.
