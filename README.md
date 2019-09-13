### Calculation of DynamoDB item
This program calculates the total size in bytes for a DynamoDB item.

Generate jar:
`# mvn clean compile assembly:single`

Usage:

`# java -jar  calc-dynamodb-item.jar <dynamo-json>`

or with pipe:
 
`# cat file.json | java -jar  calc-dynamodb-item.jar`

