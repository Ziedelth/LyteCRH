const videoContainer = document.querySelector('.video-container');
const video = document.querySelector('.video');
const indicatorElement = document.querySelector('.indicator');
const progressSlider = document.querySelector('.progress-slider');
const playPause = document.querySelector('.play-pause');
const volumeIcon = document.querySelector('#volume-icon');
const volumeSlider = document.querySelector('.volume-slider');
const durationText = document.querySelector('.duration');
const fullscreen = document.querySelector('.fullscreen');
const castElement = document.querySelector('#caster');

const cjs = new Castjs();
let castAvailable = false;
let isCasting = false;

let mouseTimeout;

let isPositionDragging = false;
let dragPosition = 0;

let isVolumeDragging = false;

const pathToVideo = 'mha/video.m3u8';

function changeTextTime() {
    if (isCasting) {
        let minutes, seconds;

        if (!isPositionDragging) {
            minutes = Math.floor(cjs.time / 60);
            seconds = Math.floor(cjs.time % 60);
        } else {
            minutes = Math.floor(dragPosition * cjs.time / 60);
            seconds = Math.floor(dragPosition * cjs.time % 60);
        }

        const durationMinutes = Math.floor(cjs.duration / 60);
        const durationSeconds = Math.floor(cjs.duration % 60);

        durationText.innerHTML = `${minutes < 10 ? '0' + minutes : minutes}:${seconds < 10 ? '0' + seconds : seconds} / ${durationMinutes < 10 ? '0' + durationMinutes : durationMinutes}:${durationSeconds < 10 ? '0' + durationSeconds : durationSeconds}`;
    } else {
        let minutes, seconds;

        if (!isPositionDragging) {
            minutes = Math.floor(video.currentTime / 60);
            seconds = Math.floor(video.currentTime % 60);
        } else {
            minutes = Math.floor(dragPosition * video.duration / 60);
            seconds = Math.floor(dragPosition * video.duration % 60);
        }

        const durationMinutes = Math.floor(video.duration / 60);
        const durationSeconds = Math.floor(video.duration % 60);

        durationText.innerHTML = `${minutes < 10 ? '0' + minutes : minutes}:${seconds < 10 ? '0' + seconds : seconds} / ${durationMinutes < 10 ? '0' + durationMinutes : durationMinutes}:${durationSeconds < 10 ? '0' + durationSeconds : durationSeconds}`;
    }
}

function updatePlayPauseButton() {
    if (video.paused) {
        playPause.innerHTML = '<i class="fas fa-play"></i>';
    } else {
        playPause.innerHTML = '<i class="fas fa-pause"></i>';
    }
}

function toggleVideo() {
    if (isCasting) {
        if (cjs.paused) {
            cjs.play();
        } else {
            cjs.pause();
        }
    } else {
        if (video.paused) {
            video.play();
        } else {
            video.pause();
        }
    }

    updatePlayPauseButton();
}

document.addEventListener("DOMContentLoaded", function () {
    video.addEventListener("contextmenu", function (event) {
        event.preventDefault();
    });

    if (!Hls.isSupported()) {
        return;
    }

    const hls = new Hls();

    hls.on(Hls.Events.MEDIA_ATTACHED, function () {
        console.log("video and hls.js are now bound together !");
    });

    hls.on(Hls.Events.MANIFEST_PARSED, function (event, data) {
        console.log("manifest loaded, found " + data.levels.length + " quality level");
    });

    // ffmpeg -i video.mp4 -codec: copy -start_number 0 -hls_time 10 -hls_list_size 0 -f hls mha/video.m3u8
    hls.loadSource(pathToVideo);
    hls.attachMedia(video);
});


// If mouse is not moving for 3 seconds, hide the indicator
videoContainer.addEventListener('mousemove', () => {
    indicatorElement.classList.remove('hidden');
    clearTimeout(mouseTimeout);

    mouseTimeout = setTimeout(() => {
        indicatorElement.classList.add('hidden');
    }, 3000);
});

video.addEventListener('loadedmetadata', () => {
    progressSlider.value = 0;
    changeTextTime();
});

video.addEventListener('timeupdate', () => {
    progressSlider.value = (video.currentTime / video.duration) * 100;
    changeTextTime();
});

video.addEventListener('volumechange', () => {
    volumeSlider.value = video.volume * 100;

    if (video.volume === 0) {
        // Remove all classes starting with 'fa-'
        volumeIcon.className = volumeIcon.className.replace(/\bfa-\S+/g, '');
        volumeIcon.classList.add('fa-volume-mute');
    } else if (video.volume > 0 && video.volume <= 0.5) {
        volumeIcon.className = volumeIcon.className.replace(/\bfa-\S+/g, '');
        volumeIcon.classList.add('fa-volume-down');
    } else {
        volumeIcon.className = volumeIcon.className.replace(/\bfa-\S+/g, '');
        volumeIcon.classList.add('fa-volume-up');
    }
});

video.addEventListener('click', toggleVideo);

document.addEventListener('keydown', (e) => {
    // If spacebar is pressed, play/pause the video
    if (e.code === 'Space') {
        toggleVideo();
    }

    // If left arrow is pressed, go back 5 seconds
    if (e.code === 'ArrowLeft') {
        video.pause();
        updatePlayPauseButton();
        video.currentTime -= 5;
    }

    // If right arrow is pressed, go forward 5 seconds
    if (e.code === 'ArrowRight') {
        video.pause();
        updatePlayPauseButton();
        video.currentTime += 5;
    }

    // If 0 numpad or 0 key is pressed, go to the beginning of the video
    if (e.code === 'Numpad0' || e.code === 'Digit0') {
        video.pause();
        updatePlayPauseButton();
        video.currentTime = 0;
    }
});

progressSlider.addEventListener('click', (e) => {
    const percent = e.offsetX / progressSlider.offsetWidth;
    video.currentTime = percent * video.duration;
});

progressSlider.addEventListener('mousedown', () => isPositionDragging = true);

progressSlider.addEventListener('mouseup', () => {
    if (!isPositionDragging) return;
    isPositionDragging = false;
    video.currentTime = dragPosition * video.duration;
});

progressSlider.addEventListener('mousemove', (e) => {
    if (isPositionDragging) {
        dragPosition = e.offsetX / progressSlider.offsetWidth;
        progressSlider.value = dragPosition * 100;
        changeTextTime();
    }
});

playPause.addEventListener('click', toggleVideo);

volumeIcon.addEventListener('click', () => {
    const sliderIsHidden = volumeSlider.classList.contains('hidden');

    if (sliderIsHidden) {
        volumeSlider.classList.remove('hidden');
    } else {
        volumeSlider.classList.add('hidden');
    }
});

volumeSlider.addEventListener('click', (e) => {
    video.volume = e.offsetX / volumeSlider.offsetWidth;
});

volumeSlider.addEventListener('mousedown', () => isVolumeDragging = true);

volumeSlider.addEventListener('mouseup', () => {
    if (!isVolumeDragging) return;
    isVolumeDragging = false;
});

volumeSlider.addEventListener('mousemove', (e) => {
    if (isVolumeDragging) {
        const dragVolume = e.offsetX / volumeSlider.offsetWidth;
        video.volume = dragVolume;
        volumeSlider.value = dragVolume * 100;
    }
});

fullscreen.addEventListener('click', () => {
    if (!document.fullscreenElement) {
        if (videoContainer.requestFullscreen) {
            videoContainer.requestFullscreen();
        }
    } else {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        }
    }
});

cjs.on('available', function () {
    console.log('Cast is available');
    castAvailable = true;
    castElement.classList.remove('hidden');
});

cjs.on('connect', function () {
    console.log('Connected to cast session');
    castElement.children[0].classList.add('main-color');
    isCasting = true;
});

cjs.on('disconnect', function () {
    console.log('Disconnected from cast session');
    castElement.children[0].classList.remove('main-color');
    isCasting = false;
});

cjs.on('timeupdate', function () {
    progressSlider.value = cjs.progress
    changeTextTime();
});

castElement.addEventListener('click', function () {
    if (castAvailable) {
        if (isCasting) {
            cjs.disconnect();
        } else {
            cjs.cast("https://media.w3.org/2010/05/sintel/trailer.mp4")
        }
    }
});

