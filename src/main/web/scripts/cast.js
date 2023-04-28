const castElement = document.querySelector('.cast');
let session = null;

initChromecast();

function initChromecast() {
    if (typeof chrome === undefined) {
        return;
    }

    const loadCastInterval = setInterval(function () {
        if (chrome.cast.isAvailable) {
            clearInterval(loadCastInterval);
            initCastApi();
        } else {
            // not available
        }
    }, 1000);
}

function sessionListener(e) {
    session = e;
    console.log('New session');

    if (session.media.length !== 0) {
        console.log('Found ' + session.media.length + ' sessions.');
    }
}

function receiverListener(e) {
    if (e === 'available') {
        console.log("Chromecast was found on the network.");
    } else {
        console.log("There are no Chromecasts available.");
    }
}

function onInitSuccess() {
    console.log("Initialization succeeded");

    buttonEvents();
    castElement.children[0].classList.add('main-color');
}

function onInitError() {
    console.log("Initialization failed");
}

function initCastApi() {
    const applicationID = chrome.cast.media.DEFAULT_MEDIA_RECEIVER_APP_ID;
    const sessionRequest = new chrome.cast.SessionRequest(applicationID);
    const apiConfig = new chrome.cast.ApiConfig(sessionRequest, sessionListener, receiverListener);
    chrome.cast.initialize(apiConfig, onInitSuccess, onInitError);
}

function onRequestSessionSuccess(e) {
    console.log("Successfully created session: " + e.sessionId);
    session = e;
    loadMedia();
}

function onLoadSuccess() {
    console.log('Successfully loaded video.');
}

function onLaunchError() {
    console.log("Error connecting to the Chromecast.");
}

function loadMedia() {
    if (!session) {
        console.log("No session.");
        return;
    }

    const mediaInfo = new chrome.cast.media.MediaInfo("https://media.w3.org/2010/05/sintel/trailer.mp4");
    mediaInfo.contentType = 'video/mp4';

    const request = new chrome.cast.media.LoadRequest(mediaInfo);
    request.autoplay = true;

    session.loadMedia(request, onLoadSuccess, onLoadError);
}

function buttonEvents() {
    castElement.addEventListener('click', function () {
        chrome.cast.requestSession(onRequestSessionSuccess, onLaunchError);

        // connectToSession()
        //     .then((castSession) => {
        //         // const mediaInfo = new chrome.cast.media.MediaInfo(pathToVideo, 'application/x-mpegURL');
        //         const mediaInfo = new chrome.cast.media.MediaInfo("https://media.w3.org/2010/05/sintel/trailer.mp4", 'video/mp4');
        //         const request = new chrome.cast.media.LoadRequest(mediaInfo);
        //         request.autoplay = true;
        //         return castSession.loadMedia(request);
        //     })
        //     .then((media) => {
        //         console.log('Media loaded:', media);
        //         listenToRemote();
        //     })
        //     .catch((err) => {
        //         console.error('Media loading error:', err);
        //     });
    });
}