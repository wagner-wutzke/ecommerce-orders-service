# Event-Driven Communication
As mentioned in the introduction, the internal communication between the ecommerce 
microservices happens based on events.\
Those events are published by services on Kafka Broker topics. Other services can listen to events 
on existing topics.