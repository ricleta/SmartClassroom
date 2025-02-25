# Navigate to the /mobile-node directory
cd ../mobile-node || { echo "Directory /mobile-node not found"; exit 1; }

echo "#---------------# Building mobile-node #---------------#"

# Run Maven clean install
mvn clean install

# Check if the build was successful
if [ $? -ne 0 ]; then
    echo "Maven build failed"
    exit 1
fi

# Find the my-mn.jar file in the target directory
JAR_FILE="target/my-mn.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found"
    exit 1
fi

# Find all directories starting with 'User' in /mobile-node
USER_DIRS=$(find . -type d -name 'User*')

if [ -z "$USER_DIRS" ]; then
    echo "No directories starting with 'User' found"
    exit 1
fi

echo "#---------------# Moving JAR file to User directories #---------------#"

# Copy the JAR file to each found directory
for USER_DIR in $USER_DIRS; do
    cp "$JAR_FILE" "$USER_DIR"
    echo "JAR file copied to $USER_DIR"
done