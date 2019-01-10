package demo.playfile;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import demo.playfile.service.PlayFileService;
import demo.playfile.util.FileItem;
import demo.playfile.util.FileUtil;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class PlayfileActivity extends Activity implements OnItemClickListener
{
	private EditText ip = null;
	private Button add = null;
	private ListView list = null;
	private ProgressDialog dialog = null;
	private ArrayAdapter<FileItem> adapter = null;
	private ArrayList<FileItem> al = new ArrayList<FileItem>();
	private SearchTask task = null;
	private String root = "/";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Intent intent = new Intent(this, PlayFileService.class);
		startService(intent);

		init();
	}

	private void init()
	{
		dialog = new ProgressDialog(this);
		dialog.setMessage("正在努力加载...");
		dialog.setCanceledOnTouchOutside(false);

		al.add(new FileItem("...", root, false));
		add = (Button) findViewById(R.id.add);
		ip = (EditText) findViewById(R.id.ip);
		list = (ListView) findViewById(R.id.list);
		adapter = new ArrayAdapter<FileItem>(this,
				android.R.layout.simple_list_item_1, al);
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);

		add.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String ipValue = ip.getText().toString();
				adapter.add(new FileItem(ipValue, "smb://" + ipValue + "/",
						false));
				root = "smb://" + ipValue + "/";
			}
		});

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id)
	{
		FileItem fileItem = al.get(position);

		if (fileItem.isFile())
		{
			String ipVal = FileUtil.ip;
			int portVal = FileUtil.port;
			String path = fileItem.getPath();
			String httpReq = "http://" + ipVal + ":" + portVal + "/smb=";
			// String httpReq = "http://" + ip + ":" + port + "/smb=";
			Log.e("=====", "" + FileUtil.ip + ":" + FileUtil.port + " " + path);
			if (path.endsWith(".mp3"))
			{
				path = path.substring(6);
				Log.e("===substring6===",path);
				try
				{
					path = URLEncoder.encode(path, "UTF-8");
				}
				catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}

				String url = httpReq + path;
				Log.e("====", "url: "+url);
				Intent it = new Intent(Intent.ACTION_VIEW);
				Uri uri = Uri.parse(url);
				it.setDataAndType(uri, "video/mp4");
				//it.setComponent(new ComponentName("com.android.music","com.android.music.MediaPlaybackActivity"));
				startActivity(it);
			

			}
			else if (path.endsWith(".mp4"))
			{
				path = path.substring(6);
				try
				{
					path = URLEncoder.encode(path, "UTF-8");
				}
				catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}

				String url = httpReq + path;
				Log.e("=====", "url: "+url);
				Intent it = new Intent(Intent.ACTION_VIEW);
				Uri uri = Uri.parse(url);
				it.setDataAndType(uri, "video/mp4");
				startActivity(it);

			}

		}
		else
		{
			String path = fileItem.getPath();
			searchFile(path);
		}

	}

	private void searchFile(String path)
	{

		if (task == null || task.getStatus().equals(AsyncTask.Status.FINISHED))
		{
			new SearchTask().execute(path);
		}

	}

	class SearchTask extends AsyncTask<String, Void, Void>
	{
		ArrayList<FileItem> item = new ArrayList<FileItem>();

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			dialog.show();
		}

		@Override
		protected Void doInBackground(String... params)
		{

			try
			{
				SmbFile smbFile = new SmbFile(params[0]);

				ArrayList<SmbFile> dirList = new ArrayList<SmbFile>();
				ArrayList<SmbFile> fileList = new ArrayList<SmbFile>();

				SmbFile[] fs = smbFile.listFiles();

				for (SmbFile f : fs)
				{
					if (f.isDirectory())
					{
						dirList.add(f);
					}
					else
					{
						fileList.add(f);
					}
				}

				dirList.addAll(fileList);

				for (SmbFile f : dirList)
				{

					String filePath = f.getPath();
					String fileName = f.getName();
					boolean isFile = f.isFile();
					Log.e("", "fileName: " + fileName + " " + filePath
							+ " isFile: " + isFile);
					item.add(new FileItem(fileName, filePath, isFile));
				}

			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
			}
			catch (SmbException e)
			{
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);

			if (!item.isEmpty())
			{
				adapter.clear();
				adapter.add(new FileItem("...", root, false));
				for (FileItem i : item)
				{
					adapter.add(i);
				}

			}
			else
			{
				Toast.makeText(PlayfileActivity.this, "加载失败了，请重试 ",
						Toast.LENGTH_SHORT).show();
			}

			dialog.cancel();
		}

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Intent intent = new Intent(this, PlayFileService.class);
		stopService(intent);
	}

}