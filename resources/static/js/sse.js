/*jshint esversion: 8 */
/*global console */

const source = new EventSource('/sse');
const eventsUl = document.getElementById('events');

function logEvent(text) {
    const li = document.createElement('li')
    li.innerText = text;
    eventsUl.appendChild(li);
}

source.addEventListener('message', function (e) {
    logEvent('message:' + e.data);
}, false);
source.addEventListener('open', function (e) {
    logEvent('open');
}, false);
source.addEventListener('error', function (e) {
    if (e.readyState == EventSource.CLOSED) {
        logEvent('closed');
    } else {
        logEvent('error');
        console.error(e);
    }
}, false);