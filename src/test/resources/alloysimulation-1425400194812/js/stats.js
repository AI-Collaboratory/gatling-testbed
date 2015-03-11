var stats = {
    type: "GROUP",
name: "Global Information",
path: "",
pathFormatted: "missing-name-b06d1",
stats: {
    "name": "Global Information",
    "numberOfRequests": {
        "total": "2",
        "ok": "0",
        "ko": "2"
    },
    "minResponseTime": {
        "total": "60015",
        "ok": "-",
        "ko": "60015"
    },
    "maxResponseTime": {
        "total": "60024",
        "ok": "-",
        "ko": "60024"
    },
    "meanResponseTime": {
        "total": "60019",
        "ok": "-",
        "ko": "60019"
    },
    "standardDeviation": {
        "total": "4",
        "ok": "-",
        "ko": "4"
    },
    "percentiles1": {
        "total": "60019",
        "ok": "-",
        "ko": "60019"
    },
    "percentiles2": {
        "total": "60021",
        "ok": "-",
        "ko": "60021"
    },
    "percentiles3": {
        "total": "60023",
        "ok": "-",
        "ko": "60023"
    },
    "percentiles4": {
        "total": "60023",
        "ok": "-",
        "ko": "60023"
    },
    "group1": {
        "name": "t < 800 ms",
        "count": 0,
        "percentage": 0
    },
    "group2": {
        "name": "800 ms < t < 1200 ms",
        "count": 0,
        "percentage": 0
    },
    "group3": {
        "name": "t > 1200 ms",
        "count": 0,
        "percentage": 0
    },
    "group4": {
        "name": "failed",
        "count": 2,
        "percentage": 100
    },
    "meanNumberOfRequestsPerSecond": {
        "total": "0.017",
        "ok": "-",
        "ko": "0.017"
    }
},
contents: {
"request-login-bf1df": {
        type: "REQUEST",
        name: "request_login",
path: "request_login",
pathFormatted: "request-login-bf1df",
stats: {
    "name": "request_login",
    "numberOfRequests": {
        "total": "1",
        "ok": "0",
        "ko": "1"
    },
    "minResponseTime": {
        "total": "60015",
        "ok": "-",
        "ko": "60015"
    },
    "maxResponseTime": {
        "total": "60015",
        "ok": "-",
        "ko": "60015"
    },
    "meanResponseTime": {
        "total": "60015",
        "ok": "-",
        "ko": "60015"
    },
    "standardDeviation": {
        "total": "0",
        "ok": "-",
        "ko": "0"
    },
    "percentiles1": {
        "total": "60015",
        "ok": "-",
        "ko": "60015"
    },
    "percentiles2": {
        "total": "60015",
        "ok": "-",
        "ko": "60015"
    },
    "percentiles3": {
        "total": "60015",
        "ok": "-",
        "ko": "60015"
    },
    "percentiles4": {
        "total": "60015",
        "ok": "-",
        "ko": "60015"
    },
    "group1": {
        "name": "t < 800 ms",
        "count": 0,
        "percentage": 0
    },
    "group2": {
        "name": "800 ms < t < 1200 ms",
        "count": 0,
        "percentage": 0
    },
    "group3": {
        "name": "t > 1200 ms",
        "count": 0,
        "percentage": 0
    },
    "group4": {
        "name": "failed",
        "count": 1,
        "percentage": 100
    },
    "meanNumberOfRequestsPerSecond": {
        "total": "0.008",
        "ok": "-",
        "ko": "0.008"
    }
}
    },"request-contain-7dd06": {
        type: "REQUEST",
        name: "request_container",
path: "request_container",
pathFormatted: "request-contain-7dd06",
stats: {
    "name": "request_container",
    "numberOfRequests": {
        "total": "1",
        "ok": "0",
        "ko": "1"
    },
    "minResponseTime": {
        "total": "60024",
        "ok": "-",
        "ko": "60024"
    },
    "maxResponseTime": {
        "total": "60024",
        "ok": "-",
        "ko": "60024"
    },
    "meanResponseTime": {
        "total": "60024",
        "ok": "-",
        "ko": "60024"
    },
    "standardDeviation": {
        "total": "0",
        "ok": "-",
        "ko": "0"
    },
    "percentiles1": {
        "total": "60024",
        "ok": "-",
        "ko": "60024"
    },
    "percentiles2": {
        "total": "60024",
        "ok": "-",
        "ko": "60024"
    },
    "percentiles3": {
        "total": "60024",
        "ok": "-",
        "ko": "60024"
    },
    "percentiles4": {
        "total": "60024",
        "ok": "-",
        "ko": "60024"
    },
    "group1": {
        "name": "t < 800 ms",
        "count": 0,
        "percentage": 0
    },
    "group2": {
        "name": "800 ms < t < 1200 ms",
        "count": 0,
        "percentage": 0
    },
    "group3": {
        "name": "t > 1200 ms",
        "count": 0,
        "percentage": 0
    },
    "group4": {
        "name": "failed",
        "count": 1,
        "percentage": 100
    },
    "meanNumberOfRequestsPerSecond": {
        "total": "0.008",
        "ok": "-",
        "ko": "0.008"
    }
}
    }
}

}

function fillStats(stat){
    $("#numberOfRequests").append(stat.numberOfRequests.total);
    $("#numberOfRequestsOK").append(stat.numberOfRequests.ok);
    $("#numberOfRequestsKO").append(stat.numberOfRequests.ko);

    $("#minResponseTime").append(stat.minResponseTime.total);
    $("#minResponseTimeOK").append(stat.minResponseTime.ok);
    $("#minResponseTimeKO").append(stat.minResponseTime.ko);

    $("#maxResponseTime").append(stat.maxResponseTime.total);
    $("#maxResponseTimeOK").append(stat.maxResponseTime.ok);
    $("#maxResponseTimeKO").append(stat.maxResponseTime.ko);

    $("#meanResponseTime").append(stat.meanResponseTime.total);
    $("#meanResponseTimeOK").append(stat.meanResponseTime.ok);
    $("#meanResponseTimeKO").append(stat.meanResponseTime.ko);

    $("#standardDeviation").append(stat.standardDeviation.total);
    $("#standardDeviationOK").append(stat.standardDeviation.ok);
    $("#standardDeviationKO").append(stat.standardDeviation.ko);

    $("#percentiles1").append(stat.percentiles1.total);
    $("#percentiles1OK").append(stat.percentiles1.ok);
    $("#percentiles1KO").append(stat.percentiles1.ko);

    $("#percentiles2").append(stat.percentiles2.total);
    $("#percentiles2OK").append(stat.percentiles2.ok);
    $("#percentiles2KO").append(stat.percentiles2.ko);

    $("#percentiles3").append(stat.percentiles3.total);
    $("#percentiles3OK").append(stat.percentiles3.ok);
    $("#percentiles3KO").append(stat.percentiles3.ko);

    $("#percentiles4").append(stat.percentiles4.total);
    $("#percentiles4OK").append(stat.percentiles4.ok);
    $("#percentiles4KO").append(stat.percentiles4.ko);

    $("#meanNumberOfRequestsPerSecond").append(stat.meanNumberOfRequestsPerSecond.total);
    $("#meanNumberOfRequestsPerSecondOK").append(stat.meanNumberOfRequestsPerSecond.ok);
    $("#meanNumberOfRequestsPerSecondKO").append(stat.meanNumberOfRequestsPerSecond.ko);
}
