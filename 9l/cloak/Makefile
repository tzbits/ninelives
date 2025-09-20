.POSIX:

# Change this to the name you want to install:
# e.g. http://localhost:8080/${PROJ}
PROJ = CloakOfDarkness
INSTALL_DIR = $(HOME)/public_html/$(PROJ)
PUB_DIR = /usr/home/jkantz1/public_html/$(PROJ)
STATIC_FILES = banner0.jpg banner1.jpg banner3.png foyer.jpg
STATIC_OUT = $(STATIC_FILES:%=out/%)
SRC_9L = $(wildcard *.9l)
SRC_JS = $(wildcard *.js)
SRC_JS_OUT = $(SRC_JS:%=out/%)
HTML_FILES = ${SRC_9L:.9l=.htm}
HTML_OUT = $(HTML_FILES:%=out/%)

all: $(STATIC_OUT) ninecups

ninecups: $(HTML_OUT) $(SRC_JS_OUT)

out/%.htm: %.9l $(SRC_JS)
	mkdir -p out
	ninecups build --js helpers.js -i ${*}.9l -o ./out/${*}.htm
	
out/%.js: $(SRC_JS)
	mkdir -p out
	cp ${*}.js ./out/${*}.js

$(STATIC_OUT): $(STATIC_FILES)
	mkdir -p out
	cp $(STATIC_FILES) out/.

install: all
	mkdir -p $(INSTALL_DIR)
	cp out/* $(INSTALL_DIR)/.
	find $(INSTALL_DIR)/ -type f | xargs chmod 644

publish: install
	rsync -azvp $(INSTALL_DIR)/ jkantz1@www.kantz.com:$(PUB_DIR)/.

clean:
	rm -Rf out

uninstall:
	rm -Rf $(INSTALL_DIR)
