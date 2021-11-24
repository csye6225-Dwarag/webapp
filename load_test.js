const http = require('http')
const options = {
    hostname: 'dev.dwarak.me',
    port: 8080,
    path: '/v1/user/self',
    method: 'GET'
}
main();
async function main(){
    for(var i=0; i<100; i++){
        console.log("Making 500 parallel calls", i);
        await load_tester();
        console.log("500 parallel calls completed", i);
    }
}async function load_tester(){
    let promises = [];
    for(let i=0; i<50; i++){
        var reqProm = new Promise(async (resolve, reject) => {
            // resolve();
            const req = await http.request(options, res => {
                // console.log(`statusCode: ${res.statusCode}`)
                Promise.resolve();        var foo = res.on('data', d => {
                    // return new Promise();
                    return resolve();
                })      })    //  req.on('response', ()=>{
            //     return Promise.resolve();
            //   })
            req.setHeader("Authorization", "Basic ZC5qYWlAZ21haWwuY29tOnJzQDEyMw==");
            req.on('error', error => {
                // console.error(error);
                resolve;
            })
            await req.end()
            // resolve();
        });  promises.push(reqProm);  }  await Promise.all(promises).then(()=>{
        console.log("ALL requests completed");
    })
}