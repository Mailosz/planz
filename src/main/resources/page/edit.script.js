
    var timeouts = {};

    var token = "@token@";
    var docId = "@document_id@";

    var genTime = document.getElementById("document-gen-time");

    var fieldErrorsSet = new Set();
    var errorResenderTimeout = null;
    var resendCounter = 0;

    var sentRequests = {};
    var resendCounter = 0;

    if (genTime != null) {
        genTime = genTime.value;

        if (genTime != null) {
            try {
                genTime = new Date(genTime);
            } catch {
                genTime = null;
            }
        }
    }

    function cancelTimeout(name) {
        let timeout = timeouts[name];
        if (timeout != null) {
            clearTimeout(timeout);
        }
        timeouts[name] = null;
    }

    function resetTimeout(name, func, time) {
        cancelTimeout(name);
        timeouts[name] = setTimeout(() =>{func(); timeouts[name] = null;}, time);
    };

    /**
     *  Sends a request and makes sure it goes through (if not displays error and tries again)
     */
    function handleRequest(id, url, options, handler) {

        let requestData = {
            id: id,
            url: url,
            options: options,
            handler: handler,
            status: "wait",
            promise: fetch(new Request(url, options)).then((resp) => {

                if (resp.ok)  {
                    requestData.status = "success";
                    resendCounter = 0;

                    let input = document.getElementById(id);
                    if (input != null) {
                        input.classList.remove("save-fail");
                    }
                } else {
                    requestData.status = "error";
                    resendCounter++;

                    console.error("Błąd żądania");
                    console.log(resp);

                    let input = document.getElementById(id);
                    if (input != null) {
                        input.classList.add("save-fail");
                    }
                }

                handler(resp);

                // resend errors
                if (errorResenderTimeout != null) {
                    clearTimeout(errorResenderTimeout);
                }

                errorResenderTimeout = setTimeout(resendErrors, 1000 * resendCounter);
                updateStatusIndicator();
            })
        }

        sentRequests[id] = requestData;
        updateStatusIndicator("wait");
    }

    
    function invokeResendErrors() {
        if (errorResenderTimeout != null) {
            clearTimeout(errorResenderTimeout);
        }

        resendCounter = 0;
        resendErrors();
    }

    function resendErrors() {

        let errorsList = [];
        for (let requestId in sentRequests) {
            let requestData = sentRequests[requestId];

            if (requestData != null && requestData.status == "error") {
                errorsList.push(requestData)
            }
        }

        if (errorsList.length > 0) {

            for (let requestData of errorsList) {
                handleRequest(requestData.id, requestData.url, requestData.options, requestData.handler);
            }
        }
    }


    function updateStatusIndicator(status) {

        if (status == null) {
            status = "success";
            for (let requestId in sentRequests) {
                let requestData = sentRequests[requestId];

                if (requestData != null) {
                    if (requestData.status == "wait") {
                        status = "wait";
                    } else if (requestData.status == "error") {
                        status = "error";
                        break;
                    }
                }
            }
        }

        let indicator = document.getElementById("status-indicator");

        if (indicator == null) {
            indicator = document.createElement("div");
            indicator.id = "status-indicator";

            let cb = document.createElement("button");
            cb.innerHTML = "&#215;";
            cb.onclick = () => {indicator.classList.add("closed");};
            indicator.appendChild(cb);

            document.body.append(indicator);
        }

        if (indicator != null) {
            indicator.classList.remove("closed");

            switch (status) {
                case "success":
                    indicator.classList.add("success");
                    indicator.classList.remove("wait");
                    indicator.classList.remove("error");
                    break;
                case "wait":
                    indicator.classList.remove("success");
                    indicator.classList.add("wait");
                    indicator.classList.remove("error");
                    break;
                case "error":
                    indicator.classList.remove("success");
                    indicator.classList.remove("wait");
                    indicator.classList.add("error");
                    break;
                default:
                    indicator.classList.remove("success");
                    indicator.classList.remove("wait");
                    indicator.classList.remove("error");
                    break;
            } 
        }
    }


    function updateFieldValue(input) {

        let url = "/values/" + docId + "/" + input.id + "?token=" + token;

        let options = {
            method: "PUT",
            body: input.value,
        };


        handleRequest(input.id, url, options, (resp) => {});


        // return fetch(request).then((resp) => {
        //     if (resp.ok) {
        //         showResultPopup("success-popup", "Zapisano zmianę");
        //         input.classList.remove("save-fail");
        //         fieldErrorsSet.delete(input);
        //         resendCounter = 0;
        //     } else {
        //         console.error("Błąd zapisu");
        //         console.log(resp);

        //         input.classList.add("save-fail");
        //         fieldErrorsSet.add(input);
        //     }
        //     invokeResendErrors();
        // });

    };


    function fieldInput(event) {
        resetTimeout(event.target.getAttribute("name"), () => updateFieldValue(event.target), 1500)
    }

    function fieldChange(event) {
        checkInput(event.target); 
        cancelTimeout(event.target.getAttribute("name"));
        updateFieldValue(event.target);
    }


    function showResultPopup(className, content) {
        let popup = document.createElement("div");
        popup.classList.add(className);
        popup.innerHTML = content;
        popup.onanimationend = (event) => document.body.removeChild(popup);
        document.body.appendChild(popup);
    }

    function checkInput(input) {
       
        let datalist = input.list;
        if (datalist == null) {
            input.classList.remove("unknown");
            return;
        }

        let value = input.value.trim().toUpperCase();
        if (value == "") {
            input.classList.remove("unknown");
            return;
        }

        for (let option of datalist.options) {
            if (value == option.value.trim().toUpperCase()) {
                
                input.classList.remove("unknown");

                return;
            }
        }
        input.classList.add("unknown");
    }

    let elems = document.querySelectorAll(".edit-hud input[type=text].user-editable-field");

        for (let elem of elems) {
            checkInput(elem);
        }

    document.body.addEventListener("load", (event) => {
        
    });


    function preventPageClosing(event) {

        for (let requestId in sentRequests) {
            let requestData = sentRequests[requestId];

            if (requestData != null && (requestData.status == "wait" || requestData.status == "error" || requestData.status == "fail")) {
                event.preventDefault();
                event.returnValue = "Zmiany nie zostały zapisane, czy na pewno chcesz wyjść?";
                break;
            }
        }



    }

    function updateDocumentContent() {
        let url = "/update/" + docId + "?token=" + token;
        let request = new Request(url, {
            method: "POST",
        });
        fetch(request).then((resp) => {
            if (resp.ok) {
                showResultPopup("success-popup", "Wykonano");

                try {
                    document.getElementById("doc-changed-label").classList.remove("doc-contains-changes");
                } catch {

                }
            } else {
                showResultPopup("fail-popup", "Błąd");
            }
            console.log(resp);
        });
    }

    document.body.onbeforeunload = preventPageClosing;
