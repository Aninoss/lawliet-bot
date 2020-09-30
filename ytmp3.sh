youtube-dl --extract-audio --audio-format mp3 --force-ipv4 --output "temp/$1_temp.%(ext)s" "https://www.youtube.com/watch?v=$1"
mv temp/$1_temp.mp3 temp/$1.mp3