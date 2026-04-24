for mod in nexus-oa nexus-erp nexus-wage; do
  file="../NexusERP-X/$mod/src/main/resources/application.yml"
  if [ -f "$file" ]; then
    sed -i '' 's/    driver-class-name: com.mysql.cj.jdbc.Driver/          driver-class-name: com.mysql.cj.jdbc.Driver/g' "$file"
    sed -i '' 's/    url: jdbc/          url: jdbc/g' "$file"
    sed -i '' 's/    username: root/          username: root/g' "$file"
    sed -i '' 's/    password: ""/          password: ""/g' "$file"
  fi
done
