ifndef VERBOSE
.SILENT:
endif

.PHONY: spa
spa:
	npm install
	npm run build
	cp dist/zpm-dashboard/browser/* src/main/resources/static/

.PHONY: jar
jar: spa
	./gradlew build

.PHONY: clean
clean:
	rm -rf build/ dist/ 2>/dev/null || true
	rm src/main/resources/static/*.html 2>/dev/null || true
	rm src/main/resources/static/*.js 2>/dev/null || true
	rm src/main/resources/static/*.css 2>/dev/null || true
