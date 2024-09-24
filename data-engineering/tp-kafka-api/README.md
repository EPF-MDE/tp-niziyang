## TP - [Apache Kafka](https://kafka.apache.org/)
### Communication problems
![](https://content.linkedin.com/content/dam/engineering/en-us/blog/migrated/datapipeline_complex.png)

### Why Kafka ?

![](https://content.linkedin.com/content/dam/engineering/en-us/blog/migrated/datapipeline_simple.png)

### Use Kafka with docker
Start multiples kafka servers (called brokers) using the docker compose recipe `docker-compose.yml` :

```bash
docker compose -f docker-compose.yml up --detach
```
Check on the docker hub the image used :
* https://hub.docker.com/r/confluentinc/cp-kafka

**Note** Using Mac M1 ? You have to change your image name `confluentinc/cp-kafka-connect:7.2.1.arm64` instead of `confluentinc/cp-kafka-connect:7.2.1`: https://github.com/provectus/kafka-ui/blob/master/documentation/compose/kafka-ui-arm64.yaml#L71

#### Verify Docker containers
```
docker ps
CONTAINER ID   IMAGE                             COMMAND                  CREATED          STATUS         PORTS                                                                                  NAMES
b015e1d06372   confluentinc/cp-kafka:7.1.3       "/etc/confluent/dock…"   10 seconds ago   Up 9 seconds   0.0.0.0:9092->9092/tcp, :::9092->9092/tcp, 0.0.0.0:9999->9999/tcp, :::9999->9999/tcp   kafka1
(...)
```

### Kafka User Interface
As Kafka does not have an interface, we are going to use the web app ["Kafka UI"](https://docs.kafka-ui.provectus.io/) thanks to docker compose.

### Kafka with an User Interface is better
As Kafka does not have an interface, we are going to use the web app ["Kafka UI"](https://docs.kafka-ui.provectus.io/) thanks to docker compose.

Using Kafka UI on http://localhost:8080/, connect to **your existing docker kafka cluster** with `localhost:9092`.

0. Using Kafka UI, create a topic "mytopic" with 5 partitions
1. Find the `mytopic` topic on Kafka UI and its different configs (InSync Replica, Replication Factor...)
2. Produce 10 messages (without a key) into it and read them
3. Look on which topic's partitions they are located.
4. Send another 10 messages but with a key called "my key"
5. Look again on which topic's partitions they are located.

Questions:
* [ ] When should we use a key when producing a message into Kafka ? What are the risks ? [Help](https://stackoverflow.com/a/61912094/3535853)

Keys should be used to produce messages when we need to ensure that messages with the same key are processed sequentially. Because Kafka guarantees that messages are ordered within each partition, messages with the same key will be routed to the same partition, which ensures that they are processed sequentially. The risk may be that the partitions are not balanced, and if the distribution of keys is not balanced, this can lead to a larger load on some partitions, which can lead to performance issues.
* [ ] How does the default partitioner (sticky partition) work with kafka ? [Help1](https://www.confluent.io/fr-fr/blog/apache-kafka-producer-improvements-sticky-partitioner/) and [Help2](https://www.conduktor.io/kafka/producer-default-partitioner-and-sticky-partitioner#Sticky-Partitioner-(Kafka-%E2%89%A5-2.4)-3)

When there is no key: By default, Sticky Partitioner sends a batch of messages to the same partition. This partition is randomized and switches to the next partition after all messages in the batch have been sent to this partition.

### Coding our own Kafka Client using Scala
Instead of using the command line interface (CLI) or Kafka UI to produce and consume, we are going to code our first app like pros.

#### Producer - the service in charge of sending messages
We are going to replace all `???` and all `//@TODO` inside `src/scala/main/com.github.polomarcus/main` and `src/scala/main/com.github.polomarcus/utils` folders.

First, Using the `scala/com.github.polomarcus/utis/KafkaProducerService`, send messages to Kafka and **read them with the Kafka UI** (Topics / Select topic name / Messages)

Questions :
* What are serializers and deserializers ? What is the one used here ? And why use them ?

Serializer: Convert in-memory objects to byte arrays.
Deserializer: converts a byte array back to an in-memory object.
We are using Deserializer here, we are using them in order to reach the data conversion, as well as to ensure the consistency of the data.

#### To run your program with SBT or Docker
There are 3 ways to run your program :
```bash
sbt "runMain com.github.polomarcus.main.MainKafkaProducer"
# OR
sbt run
# and type "4" to run "com.github.polomarcus.main.MainKafkaProducer"

# OR
docker compose run my-scala-app bash
> sbt
> run
```

After running with your sbt shell `runMain com.github.polomarcus.main.MainKafkaProducer`  you should see this kind of log :
```declarative
INFO  c.g.p.utils.KafkaProducerService$ - 
        Sending message with key "key20" and value "filter20"
```

##### Question 1
Your ops team tells your app is slow and the CPU is not used much, they were hoping to help you but they are not Kafka experts.

* [ ] Look at the method `producer.flush()` inside KafkaProducerService (Right click on "tp-kafka-api" and "Find in files" to look for it), can you improve the speed of the program ? 

Yes, by reducing the frequency of producer.flush() usage.

* [ ] What about batching the messages ? [Help](https://www.conduktor.io/kafka/kafka-producer-batching). Can this help your app performance ?

Yes, batch processing of messages reduces the number of requests (reducing network overhead), optimizes disk reads and writes, and reduces latency.

##### Question 2
Your friendly ops team warns you about kafka disks starting to be full. What can you do ?

Tips : 
* [ ] What about [messages compression](https://kafka.apache.org/documentation/#producerconfigs_compression.type) ? Can you implement it ? [You heard that snappy compression is great.](https://learn.conduktor.io/kafka/kafka-message-compression/)

yes,  props.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
Snappy compression provides a good balance between compression ratio and speed. It compresses data efficiently while maintaining reasonable performance.

* [ ] What are the downside of compression ?

Increased CPU usage, increased complexity of the data processing pipeline, and latency due to compressed and decompressed messages.

* [ ] What about [messages lifetime](https://kafka.apache.org/documentation/#topicconfigs_delete.retention.ms) on your kafka brokers ? Can you change your topic config ?



* [ ] What are the downside of increasing your messages lifetime ?

Increasing disk usage and retaining data for longer periods of time also increases the complexity of managing and querying data, which can impact performance.

##### Question 3
After a while and a lot of deployments and autoscaling (adding and removing due to traffic spikes), on your data quality dashboard you are seeing some messages are duplicated or missing. What can you do ?

Verify that acks = all, check that idempotence is configured with enable.idempotence=true

* [ ] What are ["acks"](https://kafka.apache.org/documentation/#producerconfigs_acks) ? when to use acks=0 ? when to use acks=all?
acks=0: The producer does not wait for any acknowledgement messages. In this case, the message is sent as soon as possible, but there is a risk of losing data.
acks=1: The producer waits for an acknowledgement from the Leader copy. If the Leader copy successfully receives the message but does not have time to copy it to the other copies, the message may be lost.
acks=all: the producer waits for an acknowledgement from all in-sync replicas, in which case the data is more secure because all replicas have received the message.
* [ ] Can [idempotence](https://kafka.apache.org/documentation/#producerconfigs_enable.idempotence) help us ?
Yes, Idempotency allows the producer to ensure that the same message is not written to Kafka repeatedly.By setting ENABLE_IDEMPOTENCE_CONFIG=true, the Kafka producer can avoid sending duplicate messages in possible retry scenarios, ensuring that the message will only be written once.

* [ ] what is ["min.insync.replicas"](https://kafka.apache.org/documentation/#brokerconfigs_min.insync.replicas) ?
min.insync.replicas is an important parameter used in conjunction with acks=all. It defines the minimum number of replicas that a message is considered to have been successfully written. If there are fewer synchronized copies available than this value, the producer will not be able to write data.


#### Consumer - the service in charge of reading messages
The goal is to read messages from our producer thanks to the ["KafkaConsumerService" class](https://github.com/polomarcus/tp/blob/main/data-engineering/tp-kafka-api/src/main/scala/com/github/polomarcus/utils/KafkaConsumerService.scala#L34-L35).

To run your program if you don't change anything there will be a `an implementation is missing` error, **that's normal.**
```bash
sbt "runMain com.github.polomarcus.main.MainKafkaConsumer"
> scala.NotImplementedError: an implementation is missing --> modify the `utils/KafkaConsumerService` class
```

[After modifying the code here](https://github.com/polomarcus/tp/blob/main/data-engineering/tp-kafka-api/src/main/scala/com/github/polomarcus/utils/KafkaConsumerService.scala#L34-L35) and read the code of KafkaConsumerService, you should see messages being read on your terminal.

```declarative
INFO  c.g.p.utils.KafkaProducerService$ -  Reading :
Offset : 20 from partition 0
Value : value1
Key : key1
(...)
INFO  c.g.p.utils.KafkaProducerService$ - No new messages, waiting 3 seconds
```

Open Kafka UI to the "Consumers" tab and look what new information you can get (*hint: consumer group*).

Now, resend messages to kafka thanks to `runMain com.github.polomarcus.main.MainKafkaProducer` and look at the consumer group lag inside Kafka UI.

What are we noticing ? Can we change a configuration to not read again the same data always and always ? Modify it inside our KafkaConsumerService.

There are duplicate messages, we can avoid reading the same information by disabling auto-commit props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, “false”)
to avoid reading the same information.


##### Question 1
* [ ] What happens if your consumer crash while processing data ?
* What are the "at most once" / "at least once" / "exactly once" semantics ? [Help](https://www.conduktor.io/kafka/complete-kafka-consumer-with-java#Automatic-Offset-Committing-Strategy-1)

At Most Once: A message may be delivered once or not at all. If a consumer crashes after receiving a message, but does not process the message before processing, the message will not be processed. This means that messages may be lost.
At Least Once: Messages are guaranteed to be delivered at least once, but may be delivered multiple times. If a consumer crashes after processing a message, but before acknowledging it, Kafka will resend the message, which may result in the message being processed repeatedly.
Exactly Once: Messages are guaranteed to be delivered and processed exactly once. Even if the consumer crashes or retries, Kafka and its clients ensure that each message is processed only once.

* What should we use ?

I think we should use at least once to prevent loss of information, because loss of information is unacceptable, but repeated processing is acceptable.

##### Question 2
We have introduced a bug in our program, and we would like to replay some data. Can we use Kafka UI to help our consumer group? Should we create a new consumer group ?
* [ ][Help](https://kafka.apache.org/documentation.html#basic_ops_consumer_group)

With the ConsumerGroupCommand tool, we can replay the data by listing the consumer groups: $ bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list, describing the consumer groups: $ bin/ kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-group, after which you can reset the offset by adjusting the settings.
So should we create a new consumer group? He has the advantage of bypassing bugs and not affecting the state of the current consumer group. So we can create a new consumer group with a specific offset.


#### Schema Registry - to have more control on our messages
##### Intro
Look at :
* your docker-compose.yml, and the schema-registry service.
* Inside Kafka UI, you can check the Schema registry tab, it should be empty : http://localhost:8080/ui/clusters/local/schemas

##### Questions
* [ ] What are the benefits to use a Schema Registry for messages ? [Help](https://docs.confluent.io/platform/current/schema-registry/index.html)

Schema Registry provides a powerful way to manage data structure and evolution, ensure data consistency, improve data quality, and simplify data management and version control.
* [ ] Where are stored schemas information ?

Centralized storage in a separate sub-registry.
* [ ] What is serialization ? [Help](https://developer.confluent.io/learn-kafka/kafka-streams/serialization/#serialization)

Serialization is the process of converting data from an object in memory into a format that can be stored or transmitted. For Kafka, serialization is primarily used to convert messages into a byte stream format that can be sent to a Kafka topic.

* [ ] What serialization format are supported ? [Help](https://docs.confluent.io/platform/current/schema-registry/index.html#avro-json-and-protobuf-supported-formats-and-extensibility)

Serialization formats Avro, Protobuf, and JSON Schema (called JSON_SR) are supported out of the box by Schema Registry on Confluent Cloud and Confluent Platform.
* [ ] Why is the Avro format so compact ? [Help](https://docs.confluent.io/platform/current/schema-registry/index.html#ak-serializers-and-deserializers-background)

Avro's compactness comes from its use of binary formats, omission of field names using schema definitions, support for multiple compression algorithms, embedded schemas, and optimized data representation and encoding mechanisms.
* [ ] What are the best practices to run a Schema Registry in production ? [Help1](https://docs.confluent.io/platform/current/schema-registry/index.html#sr-high-availability-single-primary) and [Help2](https://docs.confluent.io/platform/current/schema-registry/installation/deployment.html#running-sr-in-production)

By deploying a highly available Schema Registry cluster, deploy multiple Schema Registry instances and configure them to form a cluster. This ensures that even if one instance fails, the others will continue to provide services.

##### Code

[How to create a custom serializer ?](https://developer.confluent.io/learn-kafka/kafka-streams/serialization/#custom-serdes)

[Kafka Streams Data Types and Serialization](https://docs.confluent.io/platform/current/streams/developer-guide/datatypes.html#avro)

1. Inside `KafkaAvroProducerService`, discover the "@TODO" and send your first message using Avro and the Schema Registry using `runMain com.github.polomarcus.main.MainKafkaAvroProducer`
2. Inside `KafkaAvroConsumerService`, modify the 2 `???` and run `runMain com.github.polomarcus.main.MainKafkaAvroConsumer`

##### Schema Evolution
3. Add a new property to the class `News` inside the folder "src/main/scala/.../models" called `test: String` : it means a column "test" with type String
4. What happens on your console log when sending messages again with `runMain com.github.polomarcus.main.MainKafkaAvroProducer`. Why ?

Error appears, which may be caused by an incompatible Avro mode.

5. Modify the class `News` from `test: Option[String] = None`
6. Send another message and on Kafka UI Schema Registry tab, see what happens

The message with test is passed to the KafkaUI.

We've experienced a [schema evolution](https://docs.confluent.io/platform/current/schema-registry/avro.html#schema-evolution).

#### Kafka Connect 
> Kafka Connect is a free, open-source component of Apache Kafka® that works as a centralized data hub for simple data integration between databases, key-value stores, search indexes, and file systems. [Learn more here](https://docs.confluent.io/platform/current/connect/index.html)

![](https://images.ctfassets.net/gt6dp23g0g38/5vGOBwLiNaRedNyB0yaiIu/529a29a059d8971541309f7f57502dd2/ingest-data-upstream-systems.jpg)

Some videos that can be helpful :
* [Video series with Confluent](https://developer.confluent.io/learn-kafka/kafka-connect/intro/)
* [Video series with Conduktor](https://www.youtube.com/watch?v=4GSmIE9ji9c&list=PLYmXYyXCMsfMMhiKPw4k1FF7KWxOEajsA&index=25)

##### Where to find connectors ?
Already-made connectors can be found on [the Confluent Hub](https://www.confluent.io/hub/)

One has already being installed via docker-compose.yml, can you spot it ?

This File Sink connector read from a topic, and write it as a file on your local machine.

##### How to start a connector ?
For tests purposes, we are going to use the [Standalone run mode](https://docs.confluent.io/kafka-connectors/self-managed/userguide.html#standalone-mode) as we do not have a cluster.

We'll run a [FileStream connector](https://docs.confluent.io/platform/current/connect/filestream_connector.html#kconnect-long-filestream-connectors) - a connector that read from a topic and write it as a text file on our server.

To read from a specific topic, we need to configure this file [`kafka-connect-configs/connect-file-sink-properties`](https://github.com/polomarcus/tp/blob/main/data-engineering/tp-kafka-api/kafka-connect-configs/connect-file-sink.properties)

To run our FileStream connector we can operate like this (have a look to the docker-compose.yml file to understand first)
```bash
docker compose run kafka-connect bash
> ls
> cat connect-file-sink.properties
> connect-standalone connect-standalone.properties connect-file-sink.properties
```

Congrats, a file **should** have been produced on your **local** container :
```bash
ls 
cat test.sink.txt
```

**How can we use this kind of connector for a production use ( = real life cases ) ?** 
* [ ] Can we find another connector [on the Confluent Hub](https://www.confluent.io/hub/) that can write inside **a data lake** instead of a simple text file in one of our servers ?

##### How do Serializers work for Kafka connect ?
Inside `kafka-connect-config/connect-file-sink.properties`, we need to set the serializer we used to produce the data, for our case we want to use the **String Serializer** inside our config.

Tips : [Kafka Connect Deep Dive – Converters and Serialization Explained](https://www.confluent.io/fr-fr/blog/kafka-connect-deep-dive-converters-serialization-explained/)

##### How do consumer group work for Kafka connect ?
Look on Kafka UI to see if a Connector use a consumer group to bookmark partitions' offsets.

#### Kafka Streams
[Kafka Streams Intro](https://kafka.apache.org/documentation/streams/)

[Streams Developer Guide](https://docs.confluent.io/platform/current/streams/developer-guide/dsl-api.html#overview)

* [ ] What are the differences between the consumer, the producer APIs, and Kafka streams ? [Help1](https://stackoverflow.com/a/44041420/3535853)

Producer API: Responsible for writing data to Kafka topics, suitable for production data.
Consumer API: Responsible for reading data from Kafka topics, suitable for consuming data.
Kafka Streams: Provides stream processing capabilities, allowing real-time processing of streams and complex transformations.

* [ ] When to use Kafka Streams instead of the consumer API ?

We use Kafka Streams when we need to do stream partitioning tasks or when we need real-time processing, real-time analytics and machine learning.
* [ ] What is a `SerDe`?
A SerDe stands for Serializer/Deserializer,
Serializer: Convert in-memory objects to byte arrays.
Deserializer: converts a byte array back to an in-memory object.

* [ ] What is a KStream?

KStream is a data stream model in the Kafka Streams API that represents a stateless data stream. Stateless means that it does not maintain any state itself. It is a data stream that arrives sequentially and each record is processed independently.
* [ ] What is a KTable? What is a compacted topic ?


* [ ] What is a GlobalKTable?

GlobalKTable is another abstraction in Apache Kafka Streams, similar to KTable, but with a key difference: it is replicated across all instances of the application. This means that each instance has a full copy of the data, allowing for easier access to the latest state for any key, regardless of where the data was produced.

* [ ] What is a [stateful operation](https://developer.confluent.io/learn-kafka/kafka-streams/stateful-operations/) ?

Stateful operation refers to operations in stream processing that maintain some form of state across multiple records.

What are the new [configs](https://kafka.apache.org/documentation/#streamsconfigs) we can use ?


##### Code
![MapReduce](http://coderscat.com/images/2020_09_10_understanding-map-reduce.org_20200918_164359.png)

We are going to aggregate (count) how many times we receive the same word inside the topic [ConfService.TOPIC_OUT](https://github.com/polomarcus/tp/blob/main/data-engineering/tp-kafka-api/src/main/scala/com/github/polomarcus/conf/ConfService.scala#L8). For this we need to `map` every message to separate every words.

After this, we are going to filter any messages that contains `"filter"`

Then, after we filter these messages, we are going to read another topic, to join it with our aggregate


Inside `KafkaStreamsService` write code to perform :
* Write a filter function that remove every message containing the letter "filter"
* Write a function that join a KTable (our word count) with a new KStream that read from a new topic you need to create `word`.
This should display in the console the value of the KTable and the new KStream based on the same key. If we send a message with a key "value10" and the value "active", it should return key : "value10" and value (6, active), that is to say the joined values.


To execute the code you would need this command :
```bash
sbt "runMain com.github.polomarcus.main.MainKafkaStream"
```
* [ ] Stop your application once you have processed messages. If you restart your applicaton and resend messages, pay attention to your values inside the KTable. [How is the KTable state saved ?](https://docs.confluent.io/platform/current/streams/architecture.html#fault-tolerance)

The state of KTable is managed through Kafka's changelog topics and RocksDB local storage. Whenever the application is stopped or restarted, Kafka Streams restores the state of KTable from the changelog topic

Lot of examples can be found [here](https://blog.rockthejvm.com/kafka-streams/)

#### Monitoring and Operations
##### Questions
* [ ] Which metrics should we monitor once our application is deployed to the real world ?
[Datadog's Kafka dashboard overview](https://www.datadoghq.com/dashboards/kafka-dashboard/)

Broker Metrics
Topic Metrics
Consumer Metrics
Producer Metrics
Schema Registry Metrics






Have a look to the Kafka UI metrics : http://localhost:8080/ui/clusters/local/brokers/1/metrics

### Useful links
* https://sparkbyexamples.com/kafka/apache-kafka-consumer-producer-in-scala/
* https://www.confluent.io/fr-fr/blog/kafka-scala-tutorial-for-beginners/
* https://developer.confluent.io/learn-kafka/kafka-streams/get-started/
* [Hands-on Kafka Streams in Scala](https://softwaremill.com/hands-on-kafka-streams-in-scala/)
* [Scala, Avro Serde et Schema registry](https://univalence.io/blog/drafts/scala-avro-serde-et-schema-registry/)
* [Usage as a Kafka Serde (kafka lib for avro)](https://github.com/sksamuel/avro4s#usage-as-a-kafka-serde)
* [Kafka Streams example](https://blog.rockthejvm.com/kafka-streams/)
