Amazon S3 module
=============

The `aws-s3` module provides basic support for interacting with Amazon's
S3 service by providing additional handlers which sign the HTTP
request in accordance with the [S3 authentication specifications][1].  
The module also provides a convenience class for interacting with S3
Buckets. 

## Usage ##
To use the `aws-s3` module, you will need to first sign up for the S3
service.  Afterwards, Amazon should provide you with an AWS Access Key
and an AWS Secret Access Key.  You will need both of these to be able
to use the module.

Below is an example of retrieving a file from S3:

    import dispatch._
    import dispatch.s3._
    import S3._
    
    val access_key = Option(System.getenv("awsAccessKey")
    val secret_key = Option(System.getenv("awsSecretAccessKey")
    val x = h(Bucket("my-test-bucket") / "testing.txt" <@ (access_key.get, secret_key.get) as_str)

Using the `aws-s3` module, you can create buckets, delete buckets,
create files, delete files and retrieve files.  Other S3 functionality
is not provided.

To create a bucket, you can use the method `create` on a Bucket
object to create the proper Request.

    h(Bucket("my-test-bucket").create @(access_key.get, secret_key.get) >|)
    
The following snippet would delete a bucket:

    h(Bucket("my-test-bucket").DELETE @(access_key.get, secret_key.get) >|)
    
To delete a file:

    h(Bucket("my-test-bucket").DELETE / "testing.txt" <@(access_key.get, secret_key.get) >|)

Creating a file does require you to set the content type of the file upload:

    val b = Bucket("my-test-bucket")
    val testFile = new File("testing.txt")
    
    h(b / "testing.txt" <<< (testFile, "plain/text") <@(access_key.get, secret_key.get) >|)

[1]: http://docs.amazonwebservices.com/AmazonS3/index.html?RESTAuthentication.html
