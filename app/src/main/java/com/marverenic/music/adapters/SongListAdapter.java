package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.NowPlayingActivity;
import com.marverenic.music.Player;
import com.marverenic.music.PlayerService;
import com.marverenic.music.R;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class SongListAdapter extends BaseAdapter implements SectionIndexer, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {
    private ArrayList<Song> data;
    private Context context;
    private ArrayList<Character> sectionCharacter = new ArrayList<>();
    private ArrayList<Integer> sectionStartingPosition = new ArrayList<>();
    private ArrayList<Integer> sectionAtPosition = new ArrayList<>();

    public SongListAdapter(Context context) {
        this(Library.getSongs(), context);
    }

    @SuppressWarnings("unchecked")
    public SongListAdapter(ArrayList<Song> data, Context context) {
        super();
        this.data = (ArrayList<Song>) data.clone();
        this.context = context;

        String name;
        char thisChar;
        int sectionIndex = -1;
        for(int i = 0; i < data.size(); i++){
            name = data.get(i).songName.toUpperCase();

            if (name.startsWith("THE ")){
                thisChar = name.charAt(4);
            }
            else if (name.startsWith("A ")){
                thisChar = name.charAt(2);
            }
            else{
                thisChar = name.charAt(0);
            }

            if(sectionCharacter.size() == 0 || !sectionCharacter.get(sectionCharacter.size() - 1).equals(thisChar)) {
                sectionIndex++;
                sectionCharacter.add(thisChar);
                sectionStartingPosition.add(i);
            }
            sectionAtPosition.add(sectionIndex);
        }
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_song, parent, false);
        }
        Song s = data.get(position);

        if (s != null) {
            TextView tt = (TextView) v.findViewById(R.id.textSongTitle);
            TextView tt1 = (TextView) v.findViewById(R.id.textSongDetail);
            if (tt != null) {
                tt.setText(s.songName);
                tt.setTextColor(Themes.getListText());
            }
            if (tt1 != null) {
                tt1.setText(s.artistName + " - " + s.albumName);
                tt1.setTextColor(Themes.getDetailText());
            }
        } else {
            Debug.log(Debug.LogLevel.WTF, "SongListAdapter", "The requested entry is null", context);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((ListView) parent).setSelector(Themes.getTouchRipple(context));
        }

        return v;
    }

    @Override
    public int getCount() {
        if (data != null) {
            return data.size();
        } else return 0;
    }

    @Override
    public Object getItem(int position) {
        if (data != null) {
            return data.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlayerService.setQueue(context, data, position - ((ListView) parent).getHeaderViewsCount());
        PlayerService.begin();

        context.startService(new Intent(context, Player.class));
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("switchToNowPlaying", true)) {
            Navigate.to(context, NowPlayingActivity.class);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Song item = data.get(position - ((ListView) parent).getHeaderViewsCount());

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle(item.songName)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // There's nothing to do here
                    }
                })
                .setItems(R.array.queue_options_song, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: //Queue this song next
                                if (!PlayerService.isInitialized())
                                    context.startService(new Intent(context, Player.class));
                                PlayerService.queueNext(context, item);
                                break;
                            case 1: //Queue this song last
                                if (!PlayerService.isInitialized())
                                    context.startService(new Intent(context, Player.class));
                                PlayerService.queueLast(context, item);
                                break;
                            case 2: //Go to artist
                                Artist artist;

                                Cursor curArtist = context.getContentResolver().query(
                                        MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                                        null,
                                        MediaStore.Audio.Media.ARTIST + " =?",
                                        new String[]{item.artistName},
                                        MediaStore.Audio.Artists.ARTIST + " ASC");
                                curArtist.moveToFirst();

                                artist = new Artist(
                                        curArtist.getLong(curArtist.getColumnIndex(MediaStore.Audio.Artists._ID)),
                                        curArtist.getString(curArtist.getColumnIndex(MediaStore.Audio.Artists.ARTIST)));

                                curArtist.close();

                                Navigate.to(context, LibraryPageActivity.class, "entry", artist);
                                break;
                            case 3: //Go to album
                                Album album;

                                Cursor curAlbum = context.getContentResolver().query(
                                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                        null,
                                        MediaStore.Audio.Media.ALBUM + " =? AND " + MediaStore.Audio.Media.ARTIST + " =?",
                                        new String[]{item.albumName, item.artistName},
                                        MediaStore.Audio.Albums.ALBUM + " ASC");
                                curAlbum.moveToFirst();

                                album = new Album(
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums._ID)),
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)),
                                        curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)));

                                curAlbum.close();

                                Navigate.to(context, LibraryPageActivity.class, "entry", album);
                                break;
                            default:
                                break;
                        }
                    }
                });
        dialog.show();
        return true;
    }

    public void updateData(ArrayList<Song> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public Object[] getSections() {
        return sectionCharacter.toArray();
    }

    @Override
    public int getPositionForSection(int sectionNumber) {
        return sectionStartingPosition.get(sectionNumber);
    }

    @Override
    public int getSectionForPosition(int itemPosition) {
        return sectionAtPosition.get(itemPosition);
    }
}