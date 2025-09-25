IF NOT EXIST "node_modules" (
  CALL npm install --save-dev prettier @prettier/plugin-xml prettier-plugin-java
)
CALL npx prettier --plugin=@prettier/plugin-xml --plugin=prettier-plugin-java --write .
