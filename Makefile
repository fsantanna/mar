install:
	mkdir -p $(DIR)
	cp out/artifacts/mar_jar/mar.jar $(DIR)/mar.jar
	cp build/mar.sh $(DIR)/mar
	cp build/prelude.mar $(DIR)/prelude.mar
	ls -l $(DIR)/
	$(DIR)/mar --version
	$(DIR)/mar build/hello-world.mar
