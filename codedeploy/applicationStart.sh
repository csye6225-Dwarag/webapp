#sudo systemctl stop tomcat9
#sudo chmod 777 /home/ubuntu/webapp/spring-security-jpa-0.0.1-SNAPSHOT.jar
#
##java -jar /home/ubuntu/spring-security-jpa-0.0.1-SNAPSHOT.jar
#
#
#sudo systemctl stop webapp
#
#sudo systemctl start webapp
#
##sudo java -jar /home/ubuntu/webapp/spring-security-jpa-0.0.1-SNAPSHOT.jar stop
##sleep 10
##sudo kill -9 $(sudo lsof -t -i:8080)
##sleep 10
##java -jar /home/ubuntu/webapp/spring-security-jpa-0.0.1-SNAPSHOT.jar > /home/ubuntu/webapplog.txt 2> /home/ubuntu/webapplog.txt < /home/ubuntu/webapplog.txt &

sudo chmod 777 /home/ubuntu/spring-security-jpa-0.0.1-SNAPSHOT.jar
sudo chmod 777 *
sudo chown -R ubuntu:ubuntu /home/ubuntu/*
sudo systemctl stop tomcat9
sudo kill -9 $(sudo lsof -t -i:8080)
sudo systemctl start amazon-cloudwatch-agent
#sudo java -jar /home/ubuntu/spring-mvc-rest-0.0.1-SNAPSHOT.jar > /home/ubuntu/webapplog.txt &

sudo java -jar /home/ubuntu/spring-security-jpa-0.0.1-SNAPSHOT.jar > /home/ubuntu/webapplog.txt 2> /home/ubuntu/webapplog.txt < /home/ubuntu/webapplog.txt &





