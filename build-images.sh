#!/bin/sh
# Build all Ecommerce Service Maven packages and Docker images
#
set -e
#
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo "🚀 Starting build of Maven packages and Docker images for Ecommerce Services"
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
#
echo "🛠️  Building and installing Maven package for commons-domain"
echo ""
mvn -q -DskipTests -f ./ecommerce-commons-domain/pom.xml install
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
#
echo "🛠️  Building and installing Maven package for commons-data-replication"
echo ""
mvn -q -DskipTests -f ./ecommerce-commons-data-replication/pom.xml install
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
#
echo "🛠️  Building Maven package and Docker image for orders-service"
echo ""
mvn -q -DskipTests -f ./ecommerce-orders-service/pom.xml package
docker build -t orders-service:local ./ecommerce-orders-service
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
#
echo "🛠️  Building Maven package and Docker image for customers-service"
echo ""
mvn -q -DskipTests -f ./ecommerce-customers-service/pom.xml package
docker build -t customers-service:local ./ecommerce-customers-service
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
#
echo "🛠️  Building Maven package and Docker image for inventory-service"
echo ""
mvn -q -DskipTests -f ./ecommerce-inventory-service/pom.xml package
docker build -t inventory-service:local ./ecommerce-inventory-service
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
#
echo "🛠️  Building Maven package and Docker image for payments-service"
echo ""
mvn -q -DskipTests -f ./ecommerce-payments-service/pom.xml package
docker build -t payments-service:local ecommerce-payments-service
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
#
echo "🛠️  Building Maven package and Docker image for invoices-service"
echo ""
mvn -q -DskipTests -f./ecommerce-invoices-service/pom.xml package
docker build -t invoices-service:local ./ecommerce-invoices-service
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
#
echo "🛠️  Building Maven package and Docker image for shipments-service"
echo ""
mvn -q -DskipTests -f ./ecommerce-shipments-service/pom.xml package
docker build -t shipments-service:local ./ecommerce-shipments-service
echo ""
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo ""
echo "🎉 Finished build for Ecommerce Service images"
echo ""