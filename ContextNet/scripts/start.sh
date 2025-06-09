cd ..

# rebuild containers
sudo docker compose -f docker-start-gw.yml build
sudo docker compose -f contextnet-stationary.yml build

# starts gw, kafka and zookeeper in the background
sudo docker compose -f docker-start-gw.yml up -d

# starts pn and gd, showing logs in stdout
sudo docker compose -f contextnet-stationary.yml up