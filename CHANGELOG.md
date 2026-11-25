# Changelog

Versionado: `4.0.7-suiteland.N` — `4.0.7` es la base de upstream sobre la que
se rebasea el fork, `.N` es la revision propia. Los cambios de upstream que se
integran en rebases futuros se listan bajo la entrada de la revision que los
trae.

## 4.0.7-suiteland.2 (2026-11-25)

### Features

* Zero-prompt automatic updates — no confirmation screens, no risk warnings
* TOFU (trust-on-first-use) certificate validation, silent
* Unverified mods install without prompting
* Restart screen as the only exit after applying updates
* Full Spanish + English localization (all es_* dialects)
* Complete rebrand to "Suiteland Updater" — mod id `suiteland_updater`,
  data directory `suiteland_updater/`, command `/suiteland_updater`,
  thread names, log messages and Swing UI fully renamed
* Self-update channel: the updater jar ships inside the modpack it manages

### Base

* AutoModpack v4.0.7 upstream (Skidamek/AutoModpack)
