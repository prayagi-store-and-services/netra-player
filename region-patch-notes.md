# Version 1.1.12.2

Add an on-device regional availability rule for PK, BD and AF. SIM and mobile network country hints are used, with locale region only when both are empty. An IN SIM always permits use, and missing signals permit use. A denied startup shows a message and closes the app; playback service entry is denied too. No country data, IP lookup or new permission is added.

These hints do not prove physical location and can be changed or absent. This is not a security patch. Validated with unit/CI tests, not on a real phone.
