```sh
fastapi dev

mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=data --count=100 --interval=500"

mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=data --count=100 --interval=50"

mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=csv --output=mycsv --append --count=20 --interval=5"

mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args=""
```