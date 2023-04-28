# docker run -it  --mount type=bind,source="$(pwd)",target=/crunchy-cli/data --rm --name lytecrh lytecrh:latest --credentials "username:password]" download -o input.mp4 -a ja-JP -s fr-FR https://www.crunchyroll.com/fr/watch/GVWU04XQW/eyes-of-science

FROM rust as rust

# Install ffmpeg
RUN apt-get update && apt-get install -y ffmpeg

RUN git clone https://github.com/crunchy-labs/crunchy-cli.git
WORKDIR /crunchy-cli
RUN cargo build --release
RUN cargo install --force --path .

RUN mkdir /crunchy-cli/data
WORKDIR /crunchy-cli/data

ENTRYPOINT ["crunchy-cli"]