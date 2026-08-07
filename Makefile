# AudioLex device deployment via ADB Wireless Debugging (WLAN-adb, WSL2 -> Galaxy A53).
# Replaces the manual pair/connect/build/install/launch sequence documented
# in AGENTS.md Sec. 7 (Umgebung) / CLAUDE.md "Befehle".
#
# Typical flow:
#   1. On the device: Settings -> Entwickleroptionen -> Kabelloses Debugging
#      -> "Gerät mit Kopplungscode koppeln" shows IP:PAIR_PORT + a 6-digit code.
#      (Pairing survives across sessions until the device forgets the host --
#      only needed again after that, not on every deploy.)
#   2. make pair PAIR_PORT=<port> CODE=<code>
#   3. The main "Kabelloses Debugging" screen shows IP:PORT -- the connect
#      port, which changes every time wireless debugging is toggled off/on.
#   4. make deploy PORT=<port>          (connect + build + install + launch)
#
# IP is fixed for now (single known device on the home network); override
# with IP=... if that changes.

IP        ?= 192.168.178.24
PAIR_PORT ?=
CODE      ?=
PORT      ?=
PACKAGE   := de.hexenwoche.audiolex
APK       := composeApp/build/outputs/apk/debug/composeApp-debug.apk
ADB       := adb
GRADLE    := ./gradlew

.DEFAULT_GOAL := help
.PHONY: help pair connect build install launch deploy full status version disconnect

help:
	@echo "AudioLex Deployment (WLAN-ADB, Galaxy A53)"
	@echo ""
	@echo "  make pair PAIR_PORT=<port> CODE=<code>              Einmalig koppeln"
	@echo "  make deploy PORT=<port>                             Verbinden + bauen + installieren + starten"
	@echo "  make full PAIR_PORT=<port> CODE=<code> PORT=<port>  Koppeln und deployen in einem Schritt"
	@echo "  make connect PORT=<port>                            Nur verbinden"
	@echo "  make build                                          Nur Debug-APK bauen"
	@echo "  make install PORT=<port>                            Bauen + installieren"
	@echo "  make launch PORT=<port>                              App auf dem Gerät starten"
	@echo "  make status                                         Verbundene Geräte auflisten"
	@echo "  make version PORT=<port>                            Installierte Version auf dem Gerät zeigen"
	@echo "  make disconnect PORT=<port>                         Verbindung trennen"
	@echo ""
	@echo "IP ist aktuell fix auf $(IP) (überschreiben mit IP=...)."

pair:
	@if [ -z "$(PAIR_PORT)" ] || [ -z "$(CODE)" ]; then \
		echo "Fehlt: PAIR_PORT und CODE. Beispiel: make pair PAIR_PORT=45681 CODE=247817"; \
		exit 1; \
	fi
	$(ADB) pair $(IP):$(PAIR_PORT) $(CODE)

connect:
	@if [ -z "$(PORT)" ]; then \
		echo "Fehlt: PORT. Beispiel: make connect PORT=38657"; \
		exit 1; \
	fi
	$(ADB) connect $(IP):$(PORT)

build:
	$(GRADLE) :composeApp:assembleDebug

install: build
	@if [ -z "$(PORT)" ]; then \
		echo "Fehlt: PORT. Beispiel: make install PORT=38657"; \
		exit 1; \
	fi
	$(ADB) -s $(IP):$(PORT) install -r $(APK)

launch:
	@if [ -z "$(PORT)" ]; then \
		echo "Fehlt: PORT. Beispiel: make launch PORT=38657"; \
		exit 1; \
	fi
	$(ADB) -s $(IP):$(PORT) shell monkey -p $(PACKAGE) -c android.intent.category.LAUNCHER 1

deploy: connect install launch
	@echo "Deployed: $(PACKAGE) auf $(IP):$(PORT)"

full: pair deploy

status:
	$(ADB) devices -l

version:
	@if [ -z "$(PORT)" ]; then \
		echo "Fehlt: PORT. Beispiel: make version PORT=38657"; \
		exit 1; \
	fi
	@$(ADB) -s $(IP):$(PORT) shell dumpsys package $(PACKAGE) | grep -E "versionName|versionCode"

disconnect:
	@if [ -z "$(PORT)" ]; then \
		echo "Fehlt: PORT. Beispiel: make disconnect PORT=38657"; \
		exit 1; \
	fi
	$(ADB) disconnect $(IP):$(PORT)
