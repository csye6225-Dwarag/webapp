sudo systemctl stop tomcat9
#
##sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/home/ubuntu/webapp/cloudwatch-agent.json -s
##sudo systemctl restart amazon-cloudwatch-agent
#
##sudo chmod 777 /home/ubuntu/spring-security-jpa-0.0.1-SNAPSHOT.jar
#
#
#
sudo iptables -t nat -L
#
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 8080
#
## sudo mv /home/ubuntu/spring-security-jpa-0.0.1-SNAPSHOT.jar /home/ubuntu/webapp
#
#sudo mv /home/ubuntu/webapp/webapp.service  /etc/systemd/system
#
#sudo systemctl daemon-reload
#
#sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/home/ubuntu/webapp/cloudwatch-agent.json -s
#
#sudo systemctl restart amazon-cloudwatch-agent
#
#sudo systemctl stop tomcat9
sudo chmod 777 /home/ubuntu/cloudwatch-agent.json

sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/home/ubuntu/cloudwatch-agent.json -s
sudo chmod 777 amazon-cloudwatch-agent
sudo systemctl start amazon-cloudwatch-agent

sudo chmod 777 /home/ubuntu/spring-security-jpa-0.0.1-SNAPSHOT.jar
