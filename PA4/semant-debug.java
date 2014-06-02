!/bin/csh -f
set ARGS=-"agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
set SEMANT="java -classpath /usr/class/cs143/cool/lib:.:/usr/java/lib/rt.jar ${ARGS} Semant"
./lexer $* | ./parser $* | ${SEMANT} $*
