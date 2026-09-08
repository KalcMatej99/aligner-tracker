package org.alignertracker.app.photos

import android.util.Base64
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.alignertracker.app.domain.PhotoMetadata

/** Self-contained offline HTML time-lapse: accessible controls, no remote resources or upload. */
object PhotoTimeLapse {
    fun export(photos: List<PhotoMetadata>, store: PhotoStore, zone: ZoneId): String {
        require(photos.size in 2..100) { "Choose between 2 and 100 photos for a time-lapse." }
        var encodedSize = 0L
        val frames =
            photos.sortedWith(compareBy({ it.capturedAt }, { it.id })).map { photo ->
                val bytes = store.file(requireNotNull(photo.ownedFileName)).readBytes()
                encodedSize += ((bytes.size.toLong() + 2) / 3) * 4 + 128
                require(encodedSize <= 16L * 1024 * 1024) {
                    "Time-lapse is larger than 16 MiB. Select fewer photos."
                }
                require(PhotoStore.sha256(bytes) == photo.sha256) {
                    "A photo is missing or damaged."
                }
                val date =
                    DateTimeFormatter.ISO_LOCAL_DATE.format(
                        Instant.ofEpochMilli(photo.capturedAt).atZone(zone)
                    )
                "{src:'data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}',date:'$date'}"
            }
        require(frames.sumOf { it.length.toLong() } <= 16L * 1024 * 1024) {
            "Time-lapse is larger than 16 MiB. Select fewer photos."
        }
        return """<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; script-src 'unsafe-inline'"><title>Aligner Tracker photo time-lapse</title><style>body{font:1.1rem system-ui;max-width:60rem;margin:auto;padding:1rem;background:#102422;color:#fff}img{width:100%;max-height:70vh;object-fit:contain}button,input{min-height:48px;margin:.5rem;font:inherit}input{width:65%}</style><h1>Progress photo time-lapse</h1><p>Personal photographs, without diagnostic analysis. This file contains private images and works offline.</p><img id="frame" alt="Dated progress photograph"><p id="date" aria-live="polite"></p><button id="play">Play</button><label>Frame <input id="position" type="range" min="0" max="${frames.lastIndex}" value="0"></label><script>const frames=[${frames.joinToString(",")}];let timer=null;const image=document.getElementById('frame'),date=document.getElementById('date'),position=document.getElementById('position'),play=document.getElementById('play');function show(){const f=frames[Number(position.value)];image.src=f.src;image.alt='Progress photograph '+f.date;date.textContent=f.date+' · '+(Number(position.value)+1)+' / '+frames.length;}position.oninput=show;play.onclick=()=>{if(timer){clearInterval(timer);timer=null;play.textContent='Play';}else{play.textContent='Pause';timer=setInterval(()=>{position.value=(Number(position.value)+1)%frames.length;show();},1000);}};show();</script></html>"""
    }
}
