package com.example.mp3playerpro

import android.media.MediaPlayer
import android.os.Build.VERSION_CODES.M
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import com.example.mp3playerpro.databinding.ActivityPlayBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class PlayActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityPlayBinding
    val ALBUM_IMAGE_SIZE = 90
    var mediaPlayer: MediaPlayer? = null
    lateinit var musicData: MusicData
    var mp3playerJob: Job? = null
    var pauseFlag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 전달해온 intent 값을 가져옴
        musicData = intent.getSerializableExtra("musicData") as MusicData

        // 화면에 바인딩 진행
        binding.albumTitle.text = musicData.title
        binding.albumArtist.text = musicData.artist
        binding.totalDuration.text = SimpleDateFormat("mm:ss").format(musicData.duration)
        binding.playDuration.text = "00:00"
        val bitmap = musicData.getAlbumBitmap(this, ALBUM_IMAGE_SIZE)
        if (bitmap != null) {
            binding.albumImage.setImageBitmap(bitmap)
        } else {
            binding.albumImage.setImageResource(R.drawable.music_video_24)
        }
        // 음악 파일객체 가져옴
        mediaPlayer = MediaPlayer.create(this, musicData.getMusicUri())
        // 이벤트 처리(일시정지, 실행, 돌아가기, 정지, 시크바 조절)
        binding.btnPrevious.setOnClickListener(this)
        binding.listButton.setOnClickListener(this)
        binding.playButton.setOnClickListener(this)
        binding.stopButton.setOnClickListener(this)
        binding.btnNext.setOnClickListener(this)
        binding.seekBar.max = mediaPlayer!!.duration
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnPrevious -> {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, musicData.getPreviousMusicUri())
                mediaPlayer?.start()
                binding.playButton.setImageResource(R.drawable.pause_24)
                pauseFlag = false
            }
            R.id.listButton -> {
                mp3playerJob?.cancel()
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                finish()
            }
            R.id.playButton -> {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.pause()
                    binding.playButton.setImageResource(R.drawable.play_24)
                    pauseFlag = true
                } else {
                    mediaPlayer?.start()
                    binding.playButton.setImageResource(R.drawable.pause_24)
                    pauseFlag = false

                    // 코루틴으로 음악을 재생
                    val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
                    mp3playerJob = backgroundScope.launch {
                        while (mediaPlayer!!.isPlaying) {
                            var currentPosition = mediaPlayer?.currentPosition!!
                            // 코루틴속에서 화면의 값을 변동시키고자 할 때 runOnUiThread 사용
                            runOnUiThread {
                                binding.seekBar.progress = currentPosition
                                binding.playDuration.text =
                                    SimpleDateFormat("mm:ss").format(mediaPlayer?.currentPosition)
                            }
                            try {
                                delay(1000)
                                binding.seekBar.incrementProgressBy(1000)
                            } catch (e: java.lang.Exception) {
                                Log.e("PlayActivity", "delay 오류발생 ${e.printStackTrace()}")
                            }
                        }
                        if (pauseFlag == false) {
                            runOnUiThread {
                                binding.seekBar.progress = 0
                                binding.playButton.setImageResource(R.drawable.play_24)
                                binding.playDuration.text = "00:00"
                            }
                        }
                    }
                }
            }
            R.id.stopButton -> {
                mp3playerJob?.cancel()
                mediaPlayer?.stop()
                mediaPlayer = MediaPlayer.create(this, musicData.getMusicUri())
                binding.seekBar.progress = 0
                binding.playDuration.text = "00:00"
                binding.seekBar.max = mediaPlayer!!.duration
                binding.totalDuration.text = SimpleDateFormat("mm:ss").format(musicData.duration)
                binding.playButton.setImageResource(R.drawable.play_24)
            }
        }
    }

    override fun onBackPressed() {
        mp3playerJob?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        finish()
    }
}