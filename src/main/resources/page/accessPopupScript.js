function openAccessPopup(seriesId) {

    //show admin popup
    /**
     * @type {DocBuilder} 
     */
    const builder = document.builder;

    let url = "/admin/" + seriesId + "?token=" + token;
    let request = new Request(url, {
        method: "GET",
    });

    fetch(request).then((resp) => {
        console.log(resp);
        if (resp.ok) {
            return resp.json();
        } else {
            throw resp.status;
        }
    }).then((json) => {
        let content = builder.tag("div").children(
            builder.tag("div").class("admin-popup-row").attr("style", "text-align: right;").children(
                builder.tag("button").innerText("X").attr("tabindex", "10").event("click", (event) => { Popup.getPopupOfElement(event.target).hide(); })
            ),
            builder.tag("h1").innerText(json.seriesName)
        );
        let popup = new Popup(content.getTag(), {pointerDismissable: false});
        popup.show(null, "center center");
    }).catch((error) => {
        showResultPopup("fail-popup", "Błąd otwierania ( " + error + " )");
        console.error(error);
    });


}