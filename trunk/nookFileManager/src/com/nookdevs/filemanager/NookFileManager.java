package com.nookdevs.filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemClickListener;

import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.common.nookBaseActivity;

public class NookFileManager extends nookBaseActivity implements OnItemClickListener {
    
    private String[] m_StartFolders = {
        SDFOLDER, EXTERNAL_SDFOLDER
    };
    LinearLayout m_Header;
    LinearLayout m_Content;
    public static final int BROWSE = 1;
    public static final int OPEN = 2;
    public static final int SAVE = 3;
    
    private int m_Type = BROWSE;
    private FileSelectListener m_FileSelectListener = new FileSelectListener();
    private Button m_Back;
    private TextView m_Title;
    private Button m_Add;
    private boolean m_AtRoot = true;
    private Dialog m_Dialog = null;
    private String m_CurrentFolder = null;
    private TextListener m_TextListener = new TextListener();
    private ViewAnimator m_ViewAnimator = null;
    private ListView m_List = null;
    private IconArrayAdapter<CharSequence> m_LocalAdapter = null;
    private IconArrayAdapter<CharSequence> m_RemoteAdapter = null;
    private static final int CUT = 0;
    private static final int COPY = 1;
    private static final int DELETE = 2;
    private static final int RENAME = 3;
    private static final int RCOPY = 0;
    private static final int DOWNLOAD = 1;
    int[] icons = {
        R.drawable.cut, R.drawable.copy, R.drawable.delete, -1
    };
    int[] remoteicons = {
        R.drawable.copy, R.drawable.download
    };
    ImageButton m_FileIcon = null;
    public static final String APPNAME = "File Manager";
    ArrayList<RemotePC> m_Nodes = new ArrayList<RemotePC>(4);
    private NtlmPasswordAuthentication m_Auth = null;
    private boolean m_Local = true;
    private File m_Current;
    private SmbFile m_CurrentRemote;
    ImageButton m_PasteButton = null;
    private File m_CopyFile = null;
    private SmbFile m_RemoteCopy = null;
    private boolean m_CutOperation = false;
    Handler m_Handler = new Handler();
    private boolean m_Rename = false;
    private int m_CurrentNode = 0;
    private File m_externalMyDownloads = new File(EXTERNAL_SDFOLDER + "/my downloads");
    private File m_MyDownloads = new File(SDFOLDER + "/my downloads");
    private ConnectivityManager.WakeLock m_Lock = null;
    private StatusUpdater m_StatusUpdater = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        LOGTAG = "nookFileManager";
        m_Header = (LinearLayout) findViewById(R.id.header);
        m_Content = (LinearLayout) findViewById(R.id.files);
        m_Back = (Button) findViewById(R.id.back);
        m_Title = (TextView) findViewById(R.id.title);
        m_Add = (Button) findViewById(R.id.add);
        m_List = (ListView) findViewById(R.id.list);
        m_ViewAnimator = (ViewAnimator) findViewById(R.id.fileanim);
        CharSequence[] menuitems = getResources().getTextArray(R.array.localmenu);
        List<CharSequence> menuitemsList = Arrays.asList(menuitems);
        m_LocalAdapter =
            new IconArrayAdapter<CharSequence>(m_List.getContext(), R.layout.listitem, menuitemsList, icons);
        m_LocalAdapter.setImageField(R.id.ListImageView);
        m_LocalAdapter.setTextField(R.id.ListTextView);
        m_LocalAdapter.setSubTextField(R.id.ListSubTextView);
        menuitems = getResources().getTextArray(R.array.remotemenu);
        menuitemsList = Arrays.asList(menuitems);
        m_RemoteAdapter =
            new IconArrayAdapter<CharSequence>(m_List.getContext(), R.layout.listitem, menuitemsList, remoteicons);
        m_RemoteAdapter.setImageField(R.id.ListImageView);
        m_RemoteAdapter.setTextField(R.id.ListTextView);
        m_RemoteAdapter.setSubTextField(R.id.ListSubTextView);
        m_FileIcon = (ImageButton) findViewById(R.id.fileicon);
        m_List.setOnItemClickListener(this);
        m_PasteButton = (ImageButton) findViewById(R.id.paste);
        m_PasteButton.setVisibility(View.INVISIBLE);
        m_PasteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (m_CurrentFolder == null) { return; }
                if (m_CopyFile != null) {
                    CopyTask task = new CopyTask(m_CurrentFolder, m_CopyFile);
                    task.execute();
                } else if (m_RemoteCopy != null) {
                    RemoteCopyTask task = new RemoteCopyTask(m_CurrentFolder, m_RemoteCopy);
                    task.execute();
                }
            }
        });
        m_Add.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                m_Dialog = new Dialog(NookFileManager.this, android.R.style.Theme_Panel);
                if (m_AtRoot == true) {
                    m_Dialog.setContentView(R.layout.textinput);
                    EditText txt = (EditText) m_Dialog.findViewById(R.id.EditText01);
                    txt.setOnKeyListener(m_TextListener);
                    txt = (EditText) m_Dialog.findViewById(R.id.EditText02);
                    txt.setOnKeyListener(m_TextListener);
                    txt = (EditText) m_Dialog.findViewById(R.id.EditText03);
                    txt.setOnKeyListener(m_TextListener);
                } else {
                    m_Dialog.setContentView(R.layout.folderinput);
                    EditText txt = (EditText) m_Dialog.findViewById(R.id.EditText04);
                    txt.setOnKeyListener(m_TextListener);
                }
                m_Dialog.setCancelable(true);
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                m_Dialog.show();
                
            }
            
        });
        m_Add.setText(R.string.add_pc);
        m_Back.setText(R.string.back);
        if (m_Type == BROWSE) {
            m_Title.setText("");
        }
        m_Back.setOnClickListener(m_FileSelectListener);
        m_Back.setOnLongClickListener(m_FileSelectListener);
        loadFolders(m_StartFolders, true);
        loadNodes();
        ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        m_Lock = cmgr.newWakeLock(1, "nookBrowser" + hashCode());
        loadNetwork(null);
        loadStatus();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateTitle(APPNAME + " " + m_Version);
        if (m_Lock != null && !m_Lock.isHeld()) {
            m_Lock.acquire();
        }
        if (m_StatusUpdater != null) {
            m_StatusUpdater.removeCompleted();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        try {
            if (m_Lock != null && m_Lock.isHeld()) {
                m_Lock.release();
            }
        } catch (Exception ex) {
            
        }
    }
    
    private void loadStatus() {
        LayoutInflater inflater = getLayoutInflater();
        RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
        ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
        TextView text = (TextView) filedetails.findViewById(R.id.text);
        String name = getString(R.string.status);
        text.setText(name);
        icon.setImageResource(R.drawable.info);
        if (m_StatusUpdater == null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.status);
            m_StatusUpdater = new StatusUpdater(this, layout, R.layout.statustxt);
        }
        icon.setTag(m_StatusUpdater);
        icon.setOnClickListener(m_FileSelectListener);
        m_Content.addView(filedetails);
    }
    
    private void loadNodes() {
        m_Nodes.clear();
        SharedPreferences p = getPreferences(MODE_PRIVATE);
        int count = p.getInt("NODES", 0);
        if (count == 0) { return; }
        boolean holes = false;
        for (int i = 1; i <= count; i++) {
            String ip = p.getString("IP_ADDRESS" + i, "");
            if (ip.trim().equals("")) {
                holes = true;
                continue;
            }
            RemotePC pc = new RemotePC();
            pc.ip = ip;
            pc.user = p.getString("USER" + i, "");
            pc.pass = p.getString("PASS" + i, "");
            pc.idx = i;
            m_Nodes.add(pc);
        }
        if (holes) {
            Editor e = getPreferences(MODE_PRIVATE).edit();
            e.putInt("NODES", m_Nodes.size());
            for (int i = 0; i < m_Nodes.size(); i++) {
                RemotePC pc = m_Nodes.get(i);
                e.putString("IP_ADDRESS" + (i + 1), pc.ip);
                e.putString("USER" + (i + 1), pc.user);
                e.putString("PASS" + (i + 1), pc.pass);
                e.commit();
                pc.idx = i + 1;
            }
        }
    }
    
    private void loadNetwork(SmbFile folder) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            SmbFile smb = null;
            if (folder == null) {
                for (RemotePC pc : m_Nodes) {
                    RelativeLayout filedetails =
                        (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                    ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                    TextView text = (TextView) filedetails.findViewById(R.id.text);
                    text.setText(pc.ip);
                    icon.setImageResource(R.drawable.network);
                    icon.setTag(pc);
                    icon.setOnClickListener(m_FileSelectListener);
                    icon.setOnLongClickListener(m_FileSelectListener);
                    m_Content.addView(filedetails);
                }
                return;
            }
            smb = folder;
            m_Content.removeAllViews();
            SmbFile[] files = smb.listFiles();
            for (SmbFile f : files) {
                RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                TextView text = (TextView) filedetails.findViewById(R.id.text);
                String name = f.getName();
                text.setText(name);
                String type = f.isDirectory() ? "dir" : name.substring(name.lastIndexOf('.') + 1);
                int id = getResource(type);
                if (id != -1) {
                    icon.setImageResource(id);
                } else {
                    icon.setImageResource(R.drawable.image);
                }
                icon.setTag(f);
                icon.setOnClickListener(m_FileSelectListener);
                if (f.isDirectory()) {
                    icon.setOnLongClickListener(m_FileSelectListener);
                    
                }
                m_Content.addView(filedetails);
            }
        } catch (Exception ex) {
            Log.e(LOGTAG, "Exception in loadNetwork", ex);
        }
    }
    
    private void loadFolders(String[] folders, boolean base) {
        m_Content.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        if (base) {
            for (String f2 : folders) {
                RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                TextView text = (TextView) filedetails.findViewById(R.id.text);
                File f1 = new File(f2);
                String name = f1.getName();
                if (f1.getParent().equals("/")) {
                    name += " External";
                }
                text.setText(name);
                String type = "dir";
                icon.setImageResource(getResource(type));
                icon.setTag(f1);
                icon.setOnClickListener(m_FileSelectListener);
                if (f1.isDirectory()) {
                    icon.setOnLongClickListener(m_FileSelectListener);
                    
                }
                m_Content.addView(filedetails);
            }
            return;
        }
        String folder = folders[0];
        File[] files = (new File(folder)).listFiles();
        for (final File f : files) {
            RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
            ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
            TextView text = (TextView) filedetails.findViewById(R.id.text);
            String name = f.getName();
            text.setText(name);
            String type = f.isDirectory() ? "dir" : name.substring(name.lastIndexOf('.') + 1);
            int id = getResource(type);
            if (id != -1) {
                icon.setImageResource(id);
            } else {
                icon.setImageURI(Uri.parse(f.getAbsolutePath()));
            }
            icon.setTag(f);
            icon.setOnClickListener(m_FileSelectListener);
            if (f.isDirectory()) {
                icon.setOnLongClickListener(m_FileSelectListener);
                
            }
            m_Content.addView(filedetails);
        }
        
    }
    
    private int getResource(String type) {
        if ("ndir".equalsIgnoreCase(type)) { return R.drawable.network; }
        if ("dir".equalsIgnoreCase(type)) { return R.drawable.folder; }
        if ("pdb".equalsIgnoreCase(type)) { return R.drawable.pdb; }
        if ("pdf".equalsIgnoreCase(type)) { return R.drawable.pdf; }
        if ("gif".equalsIgnoreCase(type)) { return -1; }
        if ("jpg".equalsIgnoreCase(type)) { return -1; }
        if ("jpeg".equalsIgnoreCase(type)) { return -1; }
        if ("png".equalsIgnoreCase(type)) { return -1; }
        if ("epub".equalsIgnoreCase(type)) { return R.drawable.epub; }
        if ("xml".equalsIgnoreCase(type)) { return R.drawable.xml; }
        if ("txt".equalsIgnoreCase(type)) { return R.drawable.txt; }
        if ("htm".equalsIgnoreCase(type)) { return R.drawable.html; }
        if ("html".equalsIgnoreCase(type)) { return R.drawable.html; }
        if ("apk".equalsIgnoreCase(type)) { return R.drawable.icon; }
        if ("mp3".equalsIgnoreCase(type)) { return R.drawable.mp3; }
        if ("mp4".equalsIgnoreCase(type)) { return R.drawable.video; }
        return R.drawable.generic;
    }
    
    class TextListener implements OnKeyListener {
        
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                if (view instanceof EditText) {
                    EditText editTxt = (EditText) view;
                    if (keyCode == nookBaseActivity.SOFT_KEYBOARD_CLEAR) {
                        editTxt.setText("");
                    } else if (keyCode == nookBaseActivity.SOFT_KEYBOARD_SUBMIT) {
                        if (m_Rename) {
                            String name = editTxt.getText().toString();
                            if (!m_Current.renameTo(new File(m_Current.getParent() + "/" + name))) {
                                displayError(R.string.rename_error);
                            }
                            m_Dialog.cancel();
                            m_Rename = false;
                            clickAction(m_Back);
                            return false;
                        }
                        if (m_AtRoot) {
                            RemotePC pc = new RemotePC();
                            pc.ip = ((EditText) (m_Dialog.findViewById(R.id.EditText01))).getText().toString();
                            pc.user = ((EditText) (m_Dialog.findViewById(R.id.EditText02))).getText().toString();
                            pc.pass = ((EditText) (m_Dialog.findViewById(R.id.EditText03))).getText().toString();
                            // add pc
                            int count = getPreferences(MODE_PRIVATE).getInt("NODES", 0);
                            Editor e = getPreferences(MODE_PRIVATE).edit();
                            count++;
                            pc.idx = count;
                            e.putString("IP_ADDRESS" + count, pc.ip);
                            e.putString("USER" + count, pc.user);
                            e.putString("PASS" + count, pc.pass);
                            e.putInt("NODES", count);
                            e.commit();
                            m_Nodes.add(pc);
                            loadFolders(m_StartFolders, true);
                            loadNetwork(null);
                            loadStatus();
                        } else {
                            // create folder.
                            String foldername = editTxt.getText().toString();
                            File f = new File(m_CurrentFolder + "/" + foldername + "/");
                            f.mkdir();
                            String[] subfolder = {
                                m_CurrentFolder
                            };
                            loadFolders(subfolder, false);
                        }
                        m_Dialog.cancel();
                        
                    } else if (keyCode == nookBaseActivity.SOFT_KEYBOARD_CANCEL) {
                        if (m_Rename) {
                            m_Rename = false;
                            clickAction(m_Back);
                        }
                        m_Dialog.cancel();
                    }
                }
            }
            return false;
        }
    }
    
    private boolean m_FileView = false;
    private boolean m_DirDetails = false;
    private boolean m_StatusView = false;
    
    private void clickAction(View v) {
        m_Current = null;
        m_CurrentRemote = null;
        if (v.getTag() == null) {
            if (m_DirDetails) {
                finish();
            } else {
                goBack();
            }
            return;
        }
        m_PasteButton.setVisibility(View.INVISIBLE);
        if (v.getTag() instanceof StatusUpdater) {
            m_Back.setText(" < ");
            m_Back.setTag(new File("/"));
            m_Add.setVisibility(View.INVISIBLE);
            m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromright);
            m_StatusView = true;
            m_Title.setText(R.string.status);
            m_Back.setText("<");
            m_ViewAnimator.showPrevious();
            return;
        } else if (m_StatusView) {
            m_StatusView = false;
            m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromleft);
            m_ViewAnimator.showNext();
        }
        if (m_FileView) {
            m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromleft);
            m_ViewAnimator.showPrevious();
        }
        if (v.getTag() instanceof RemotePC) {
            try {
                m_Back.setText(" < ");
                m_Back.setTag(new File("/"));
                m_Add.setVisibility(View.INVISIBLE);
                RemotePC pc = (RemotePC) v.getTag();
                m_Title.setText(pc.ip);
                if (m_DirDetails) { // file details.
                    m_List.setAdapter(m_LocalAdapter);
                    m_FileIcon.setImageDrawable(((ImageButton) v).getDrawable());
                    m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromright);
                    m_ViewAnimator.showNext();
                    m_DirDetails = false;
                    m_FileView = true;
                    m_Local = true;
                    m_CurrentNode = pc.idx;
                    return;
                }
                String smUrl = "smb://";
                if (pc.user != null && !pc.user.trim().equals("")) {
                    System.setProperty("jcifs.smb.client.password", pc.pass);
                    System.setProperty("jcifs.smb.client.user", pc.user);
                    smUrl += pc.user;
                    smUrl += ":";
                    smUrl += pc.pass + "@";
                } else {
                    m_Auth = null;
                }
                SmbFile sf = new SmbFile(smUrl + pc.ip + "/");
                loadNetwork(sf);
                return;
            } catch (Exception ex) {
                Toast.makeText(NookFileManager.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(LOGTAG, "Exception connecting to remote PC - ", ex);
                return;
            }
        }
        if (v.getTag() instanceof SmbFile) {
            SmbFile sf = (SmbFile) v.getTag();
            m_Add.setText(R.string.add_folder);
            m_Add.setVisibility(View.INVISIBLE);
            m_AtRoot = false;
            try {
                String parent = sf.getParent();
                if (parent.endsWith("/")) {
                    parent = parent.substring(0, parent.length() - 1);
                }
                int idx = parent.lastIndexOf('/');
                parent = parent.substring(idx + 1);
                m_Back.setText(" < " + parent);
                if (parent.trim().equals("")) {
                    m_Back.setTag(new File("/"));
                } else {
                    m_Back.setTag(new SmbFile(sf.getParent()));
                }
                m_Title.setText(sf.getName());
                if (!m_DirDetails && sf.isDirectory()) {
                    loadNetwork(sf);
                    m_FileView = false;
                } else {
                    // file details.
                    m_FileView = true;
                    m_List.setAdapter(m_RemoteAdapter);
                    m_FileIcon.setImageDrawable(((ImageButton) v).getDrawable());
                    m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromright);
                    m_ViewAnimator.showNext();
                    m_DirDetails = false;
                    m_Local = false;
                    m_FileView = true;
                    m_CurrentRemote = sf;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.e(LOGTAG, "error in listener ", e);
            }
            return;
        }
        File f = (File) v.getTag();
        m_CurrentFolder = null;
        if (f.getParent() == null) {
            m_Back.setText(R.string.back);
            m_Back.setTag(null);
            if (m_Type == BROWSE) {
                m_Title.setText("");
            }
            m_Add.setText(R.string.add_pc);
            m_AtRoot = true;
            if (f.isDirectory()) {
                m_Add.setVisibility(View.VISIBLE);
                loadFolders(m_StartFolders, true);
                loadNetwork(null);
                loadStatus();
                m_FileView = false;
            }
            return;
        } else {
            m_Add.setText(R.string.add_folder);
            m_AtRoot = false;
            String tmp = f.getParent();
            m_CurrentFolder = f.getAbsolutePath();
            boolean valid = false;
            for (String t : m_StartFolders) {
                if (tmp.contains(t)) {
                    valid = true;
                    break;
                }
            }
            if (valid) {
                m_Back.setTag(f.getParentFile());
                m_Back.setText(" < " + f.getParentFile().getName());
            } else {
                m_Back.setTag(new File("/"));
                m_Back.setText(" < " + f.getParentFile().getName());
            }
        }
        String[] subfolder = {
            f.getAbsolutePath()
        };
        m_Title.setText(f.getName());
        if (f.getName().equals("sdcard")) {
            m_DirDetails = false;
        }
        if (!m_DirDetails && f.isDirectory()) {
            m_Add.setVisibility(View.VISIBLE);
            loadFolders(subfolder, false);
            m_FileView = false;
            if (m_CopyFile != null && m_CopyFile.exists()) {
                m_PasteButton.setVisibility(View.VISIBLE);
            } else {
                m_CopyFile = null;
            }
            try {
                if (m_RemoteCopy != null && m_RemoteCopy.exists()) {
                    m_PasteButton.setVisibility(View.VISIBLE);
                } else {
                    m_RemoteCopy = null;
                }
            } catch (Exception ex) {
                m_RemoteCopy = null;
            }
        } else {
            // file details.
            m_Add.setVisibility(View.INVISIBLE);
            m_FileIcon.setImageDrawable(((ImageButton) v).getDrawable());
            m_List.setAdapter(m_LocalAdapter);
            m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromright);
            m_ViewAnimator.showNext();
            m_Local = true;
            m_FileView = true;
            m_DirDetails = false;
            m_Current = f;
        }
    }
    
    class FileSelectListener implements OnClickListener, OnLongClickListener {
        
        public void onClick(View v) {
            clickAction(v);
        }
        
        public boolean onLongClick(View v) {
            m_DirDetails = true;
            return false;
        }
    }
    
    public void onItemClick(AdapterView<?> view, View parent, int position, long id) {
        if (m_Local) {
            switch (position) {
                case CUT:
                    if (m_Current == null) {
                        displayError(R.string.operation_invalid);
                        return;
                    }
                    m_CutOperation = true;
                case COPY:
                    if (m_Current == null) {
                        displayError(R.string.operation_invalid);
                        return;
                    }
                    m_CopyFile = m_Current;
                    m_RemoteCopy = null;
                    m_PasteButton.setVisibility(View.VISIBLE);
                    clickAction(m_Back);
                    break;
                case DELETE:
                    if (m_Current == null) {
                        Editor e = getPreferences(MODE_PRIVATE).edit();
                        e.putString("IP_ADDRESS" + m_CurrentNode, "");
                        e.commit();
                        loadNodes();
                        clickAction(m_Back);
                        
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.delete);
                        builder.setMessage(R.string.confirm);
                        builder.setNegativeButton(android.R.string.no, null).setCancelable(true);
                        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (!deleteDir(m_Current)) {
                                    displayError(R.string.delete_error);
                                }
                                clickAction(m_Back);
                            }
                        });
                        builder.show();
                    }
                    break;
                case RENAME:
                    if (m_Current == null) {
                        displayError(R.string.operation_invalid);
                        return;
                    }
                    m_Dialog = new Dialog(NookFileManager.this, android.R.style.Theme_Panel);
                    m_Dialog.setContentView(R.layout.folderinput);
                    EditText txt = (EditText) m_Dialog.findViewById(R.id.EditText04);
                    txt.setOnKeyListener(m_TextListener);
                    m_Dialog.setCancelable(true);
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    m_Rename = true;
                    m_Dialog.show();
            }
        } else {
            if (position == 0) {
                // plain copy
                m_RemoteCopy = m_CurrentRemote;
                m_CopyFile = null;
                m_PasteButton.setVisibility(View.VISIBLE);
                clickAction(m_Back);
            } else {
                String target = "";
                if (m_externalMyDownloads.exists()) {
                    target = m_externalMyDownloads.getAbsolutePath();
                } else {
                    target = m_MyDownloads.getAbsolutePath();
                    if (!m_MyDownloads.exists()) {
                        m_MyDownloads.mkdir();
                    }
                }
                RemoteCopyTask task = new RemoteCopyTask(target, m_CurrentRemote);
                clickAction(m_Back);
                task.execute();
            }
            
        }
        
    }
    
    private void displayError(final int resid) {
        m_Handler.post(new Runnable() {
            public void run() {
                Toast.makeText(NookFileManager.this, resid, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void displayInfo(final int resid) {
        m_Handler.post(new Runnable() {
            public void run() {
                Toast.makeText(NookFileManager.this, resid, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String element : children) {
                boolean success = deleteDir(new File(dir, element));
                if (!success) { return false; }
            }
        }
        
        // The directory is now empty so delete it
        return dir.delete();
    }
    
    private void copyRemoteDirectory(SmbFile sourceLocation, File targetLocation, RemoteCopyTask task)
        throws IOException {
        
        if (sourceLocation.isDirectory()) {
            sourceLocation = new SmbFile(sourceLocation.getCanonicalPath() + "/");
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (String element : children) {
                copyRemoteDirectory(new SmbFile(sourceLocation, element), new File(targetLocation, element), task);
            }
        } else {
            
            SmbFileInputStream in = new SmbFileInputStream(sourceLocation);
            FileOutputStream out = new FileOutputStream(targetLocation);
            
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            float size = sourceLocation.length();
            float current = 0;
            int len;
            int prevProgress = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                current += len;
                int progress = (int) (current / size * 99);
                if (progress > 99) {
                    progress = 99;
                }
                if (prevProgress != progress) {
                    task.updateProgress(progress);
                    prevProgress = progress;
                }
            }
            in.close();
            out.close();
        }
    }
    
    private void copyDirectory(File sourceLocation, File targetLocation, CopyTask task) throws IOException {
        
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (String element : children) {
                copyDirectory(new File(sourceLocation, element), new File(targetLocation, element), task);
            }
        } else {
            FileInputStream in = new FileInputStream(sourceLocation);
            FileOutputStream out = new FileOutputStream(targetLocation);
            float size = sourceLocation.length();
            float current = 0;
            int prevProgress = 0;
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                current += len;
                int progress = (int) (current / size * 99);
                if (progress > 99) {
                    progress = 99;
                }
                if (prevProgress != progress) {
                    task.updateProgress(progress);
                    prevProgress = progress;
                }
            }
            in.close();
            out.close();
        }
    }
    
    class RemoteCopyTask extends AsyncTask<Void, Integer, Boolean> {
        File m_Target = null;
        String m_CurrFolder = null;
        SmbFile m_Copy;
        int m_Id;
        
        public RemoteCopyTask(String currFolder, SmbFile copy) {
            m_CurrFolder = currFolder;
            m_Copy = copy;
        }
        
        @Override
        protected void onPreExecute() {
            try {
                m_Target = null;
                if (m_Copy == null) { return; }
                if (m_CurrFolder == null) { return; }
                m_Target = new File(m_CurrFolder + "/" + m_Copy.getName());
                m_Id = m_StatusUpdater.addFile(m_Target.getAbsolutePath());
                if (m_Target.exists()) {
                    displayError(R.string.already_exists);
                    m_Target = null;
                    return;
                }
            } catch (Exception ex) {
                displayError(R.string.copy_failed);
                m_Target = null;
            }
        }
        
        public void updateProgress(Integer... params) {
            super.publishProgress(params);
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            m_StatusUpdater.updateProgress(m_Id, progress[0]);
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            ConnectivityManager.WakeLock lock = cmgr.newWakeLock(1, "NookFileManager.remoteCopyTask" + hashCode());
            try {
                if (m_Target == null) { return false; }
                lock.acquire();
                copyRemoteDirectory(m_Copy, m_Target, this);
                try {
                    if (m_CutOperation) {
                        m_Copy.delete();
                    }
                } catch (Exception ex) {
                    Log.e(LOGTAG, "Failed to delete file after copy.", ex);
                }
                m_RemoteCopy = null;
                m_CutOperation = false;
                lock.release();
                return true;
            } catch (Exception ex) {
                Log.e(LOGTAG, "remote copy failed...", ex);
                if (lock.isHeld()) {
                    lock.release();
                }
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                displayInfo(R.string.copy_success);
                m_StatusUpdater.updateProgress(m_Id, 100);
                if (m_CurrentFolder != null && m_CurrentFolder.equals(m_Target.getParent())) {
                    String[] subfolder = {
                        m_CurrentFolder
                    };
                    loadFolders(subfolder, false);
                }
                if (m_CopyFile == null || !m_CopyFile.exists()) {
                    m_PasteButton.setVisibility(View.INVISIBLE);
                }
            } else if (m_Target != null) {
                displayError(R.string.copy_failed);
                m_StatusUpdater.updateProgress(m_Id, -1);
            } else {
                m_StatusUpdater.updateProgress(m_Id, -1);
            }
            
        }
    }
    
    class CopyTask extends AsyncTask<Void, Integer, Boolean> {
        File m_Target = null;
        String m_CurrFolder = null;
        File m_Copy = null;
        int m_Id;
        
        public CopyTask(String currFolder, File copy) {
            m_CurrFolder = currFolder;
            m_Copy = copy;
        }
        
        @Override
        protected void onPreExecute() {
            try {
                m_Target = null;
                if (m_Copy == null) { return; }
                if (m_CurrFolder == null) { return; }
                m_Target = new File(m_CurrFolder + "/" + m_Copy.getName());
                m_Id = m_StatusUpdater.addFile(m_Target.getAbsolutePath());
                if (m_Target.exists()) {
                    displayError(R.string.already_exists);
                    m_Target = null;
                    return;
                }
            } catch (Exception ex) {
                displayError(R.string.copy_failed);
                m_Target = null;
            }
        }
        
        public void updateProgress(Integer... params) {
            super.publishProgress(params);
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            m_StatusUpdater.updateProgress(m_Id, progress[0]);
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            ConnectivityManager.WakeLock lock = cmgr.newWakeLock(1, "NookFileManager.copyTask" + hashCode());
            try {
                if (m_Target == null) { return false; }
                lock.acquire();
                copyDirectory(m_Copy, m_Target, this);
                try {
                    if (m_CutOperation) {
                        m_Copy.delete();
                    }
                } catch (Exception ex) {
                    Log.e(LOGTAG, "Failed to delete file after copy.", ex);
                }
                m_CutOperation = false;
                lock.release();
                return true;
            } catch (Exception ex) {
                Log.e(LOGTAG, "File copy failed...", ex);
                if (lock.isHeld()) {
                    lock.release();
                }
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                displayInfo(R.string.copy_success);
                m_StatusUpdater.updateProgress(m_Id, 100);
                if (m_CurrentFolder != null && m_CurrentFolder.equals(m_Target.getParent())) {
                    String[] subfolder = {
                        m_CurrentFolder
                    };
                    loadFolders(subfolder, false);
                }
                if (m_CopyFile == null || !m_CopyFile.exists()) {
                    m_PasteButton.setVisibility(View.INVISIBLE);
                    m_CopyFile = null;
                }
            } else if (m_Target != null) {
                displayError(R.string.copy_failed);
                m_StatusUpdater.updateProgress(m_Id, -1);
            } else {
                m_StatusUpdater.updateProgress(m_Id, -1);
            }
        }
    }
    
    class RemotePC {
        String ip;
        String user;
        String pass;
        int idx;
    }
}