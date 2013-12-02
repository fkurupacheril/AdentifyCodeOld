var util = require("util"),  
    http = require("http"),  
    url = require("url"),  
    path = require("path"),  
    fs = require("fs");  
	var credentials = {AccessKeyId : "AKIAIVMZKKPJPKFR7VRA", 
					   SecretKey   : "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"}; 
	var dynamoDB = require('./lib/dynamoDB').DynamoDB(credentials);
	

/**************************************************************
* Function to return the HTML file specified in the URL.      *
* Example: If someone invokes http://mynodeapp.com            *
* the code will make a call to load_static_file('index.html') *
* to return back the default home page.                       *
**************************************************************/
function load_static_file(uri, response) {  
    var filename = path.join(process.cwd(), uri);  
    var ext = filename.substring(filename.lastIndexOf('.') + 1);
    
    path.exists(filename, function(exists) {  
        if(!exists) {  
            response.writeHead(404, {"Content-Type": "text/plain"});  
            response.write("404 Not Found\n");  
            response.end();  
            return;  
        }  
  
        fs.readFile(filename, "binary", function(err, file) {  
            if(err) {  
            console.log("screwing up here");
                response.writeHead(500, {"Content-Type": "text/plain"});  
                response.write(err + "\n");  
                response.end();  
                return;  
            }  
            
            if (ext == "3gp") { 
                response.writeHead(200, {"Content-Type": "audio/3gpp" });
                response.end();
                
            }
            else {
               response.writeHead(200);  
               response.write(file, "binary");  
               response.end();
            }  
        });  
    });  
}  

/*
{"StartTime":{"N":"300"},"RowNumber":{"N":"2"},"Recording":{"S":"$$$$"},"EndTime":{"N":"400"},"UserName":{"S":"Ed Mondragan"}}
*/

function sendResponse(tableName,response) {
    var responseData = "";
	var users_array = [];
	var responseArray = [];

	dynamoDB.scan(
			{"TableName"    :tableName}
				, function(resp,result) {
					resp.on('data', function(chunk){
						//console.log(""+chunk);
					});
					result.on('ready', function(data){
					
						clearTimeout(timeout);
						response.writeHead(200, { "Content-Type" : "text/HTML" });  
						
						var numberEntries = data.Items.length;
						response.write("<HTML>");
					
		                    for (var entryNumber=0; entryNumber < numberEntries; entryNumber++) {   						
							response.write(JSON.stringify(data.Items[entryNumber].UserName)+"<br>");
							response.write(JSON.stringify(data.Items[entryNumber].RowNumber)+"<br>");
							response.write(JSON.stringify(data.Items[entryNumber].StartTime)+"<br>");
							response.write(JSON.stringify(data.Items[entryNumber].EndTime)+"<br>");
						    
							var fs = require('fs'); 
							var crypto = require('crypto');
							var filename = 'foo'+crypto.randomBytes(4).readUInt32LE(0)+'bar.3gp';
							var audioBuffer = new Buffer((data.Items[entryNumber].Recording.B), 'base64');
									
							fs.writeFileSync(filename, audioBuffer, function (err) {
  										if (err) throw err;
  										console.log('It\'s saved!');

							});
							
						    response.write("<A HREF=./"+filename+">Click to Hear Recording</A><br><br>");
						   }
						 		
				   	    response.write("</HTML>");
						response.end(); 
					});
					
				});

				var timeout = setTimeout(function() {  
					response.writeHead(200, { "Content-Type" : "application/JSON" });  
					response.write(JSON.stringify([]));  
					response.end();  
					}, 10000); 	
}

/*************************************************************
* Global Exception Handler for unCaughtException in Node     *
**************************************************************/
process.on('uncaughtException', function (err) {
  console.log('Caught exception: ' + err);
});

/*************************************************************
* Main Server logic to process each incoming HTTP Request    *
*                                                            *
* Logic:                                                     *
* 1. Parse the incoming request url                          *
* 2. if '/', return back the index.html                      *
* 3. if '/service', return back the JSON result              *
* 4. Anything else, return back the Error                    *
**************************************************************/
http.createServer(function(request, response) {  
    var uri = url.parse(request.url).pathname;  
    console.log(uri);
	var responseData = "";
	var ext = uri.substring(uri.lastIndexOf('.') + 1);
	
	if (uri === "/") {
	   load_static_file('/index.html',response);
	}
	else if (uri === "/index.html") {
	   load_static_file('/index.html',response);
	}
	else if (uri === "/listener_data") {
		sendResponse("Ad-entify-listener-data",response);
	}
	else if (uri === "/Original_266x75.png") {
	   load_static_file('/Original_266x75.png',response);
	}
	else if (uri === "/favicon.ico") {
	   load_static_file('/favicon.ico',response);
	}
    else if (ext == "3gp") { 
    	   load_static_file(uri,response);
         }
	/*load_static_file(uri,response);
	*//*
	else {
	   load_static_file('/error.html',response);
	}*/
}).listen(process.env.VCAP_APP_PORT || 3000);  	 
console.log('AdEntify Dashboard has started listening on port 3000');