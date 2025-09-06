IF NOT EXIST "node_modules" (
  npm install --save-dev prettier @prettier/plugin-xml prettier-plugin-java
)
npx prettier --plugin=@prettier/plugin-xml --plugin=prettier-plugin-java --write .
