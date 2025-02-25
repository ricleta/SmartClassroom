sudo docker stop $(sudo docker ps -a -f "name=fleury" -q)
sudo docker rm $(sudo docker ps -a -f "name=fleury" -q)
