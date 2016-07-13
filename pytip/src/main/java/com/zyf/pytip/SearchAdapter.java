package com.zyf.pytip;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SearchAdapter<T> extends BaseAdapter implements Filterable {

    private Context mContext;
    private int mResource;//AutoCompleteTextView每一项所对应的布局文件
    private List<T> mObjects;//所有显示在AutoCompleteTextView中的汉字的集合
    private int mFieldId = 0;

    private int maxMatch = 10;//最多显示多少个可能选项

    private List<Set<String>> pinyinList;//AutoCompleteTextView显示的每个汉字的首字母。支持多音字,类似:{{z,c},{j},{z},{q,x}}的集合

    //支持多音字
    public SearchAdapter(Context context, int textViewResourceId, T[] objects, int maxMatch) {
        init(context, textViewResourceId, 0, Arrays.asList(objects));
        try {
            this.pinyinList = toPinYin((String[]) objects);
        } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
            badHanyuPinyinOutputFormatCombination.printStackTrace();
        }
        System.out.println(pinyinList);
        try {
            List<Set<String>> sets321 = toPinYin((String[]) objects);
            System.out.println(sets321);
        } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
            badHanyuPinyinOutputFormatCombination.printStackTrace();
        }


        this.maxMatch = maxMatch;
    }

    private void init(Context context, int resource, int textViewResourceId, List<T> objects) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
        mObjects = objects;
        mFieldId = textViewResourceId;
    }

    public int getCount() {
        return mObjects.size();
    }

    public T getItem(int position) {
        return mObjects.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        TextView text;

        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }

        try {
            if (mFieldId == 0) {
                text = (TextView) view;
            } else {
                text = (TextView) view.findViewById(mFieldId);
            }
        } catch (ClassCastException e) {
            Log.e("ArrayAdapter",
                    "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        text.setText(getItem(position).toString());

        return view;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 获得汉字拼音首字母列表
    public List<Set<String>> toPinYin(String[] hanzis) throws BadHanyuPinyinOutputFormatCombination {
        List<Set<String>> res = new ArrayList<>();

        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITH_TONE_NUMBER);

        for (String hanzi : hanzis) {
            Set<String> set = new HashSet<>();
            String[][] str = new String[hanzi.length()][];//为第一维开辟空间
            for (int i = 0; i < hanzi.length(); i++) {
                char temp = hanzi.charAt(i);
                if (temp >= 0x4e00 && temp <= 0x9fa5) {  //是否在汉字范围内
                    String[] pinyinArray = net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(temp, format);
                    str[i] = new String[pinyinArray.length];//为第二维开辟空间
                    for (int j = 0; j < pinyinArray.length; j++) {//如果是多音字的话，放的时候要求不重复
                        str[i][j] = pinyinArray[j].substring(0, 1);
                    }
                }
            }
            String[] ssss = paiLie(str);
            for (int i = 0; i < ssss.length; i++)
                set.add(ssss[i]);
            res.add(set);
        }
        return res;    //将汉字返回
    }

    /*
     * 求2维数组所有排列组合情况
     * 比如:{{1,2},{3},{4},{5,6}}共有2中排列,为:1345,1346,2345,2346
     */
    private String[] paiLie(String[][] str) {
        int max = 1;
        for (int i = 0; i < str.length; i++) {
            max *= str[i].length;
        }
        String[] result = new String[max];
        for (int i = 0; i < max; i++) {
            String s = "";
            int temp = 1;      //注意这个temp的用法。
            for (int j = 0; j < str.length; j++) {
                temp *= str[j].length;
                s += str[j][i / (max / temp) % str[j].length];
            }
            result[i] = s;
        }

        return result;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private LayoutInflater mInflater;

    public static final int ALL = -1;//全部

    /////////////////////////////////////////////////////////////////////////////////////////
    private final Object mLock = new Object();//线程锁

    private ArrayFilter mFilter;
    private ArrayList<T> mOriginalValues;//所有显示到AutoCompleteTextView中的汉字

    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {//prefix前缀
            FilterResults results = new FilterResults();//所有显示到AutoCompleteTextView中的汉字,以及汉字的个数

            if (mOriginalValues == null) {
                synchronized (mLock) { //同步代码代码块
                    mOriginalValues = new ArrayList<T>(mObjects);//
                }
            }

            if (prefix == null || prefix.length() == 0) {
                synchronized (mLock) {
                    ArrayList<T> list = new ArrayList<T>(mOriginalValues);//List<T>
                    results.values = list;
                    results.count = list.size();
                }
            } else {
                String prefixString = prefix.toString().toLowerCase();

                final ArrayList<T> hanzi = mOriginalValues;//所有显示到AutoCompleteTextView中的汉字
                final int count = hanzi.size();

                final Set<T> newValues = new HashSet<T>(count);//支持多音字,不重复

                for (int i = 0; i < count; i++) {
                    final T value = hanzi.get(i);//汉字String
                    final String valueText = value.toString().toLowerCase();//汉字String
                    final Set<String> pinyinSet = pinyinList.get(i);//支持多音字,类似:{z,c}
                    Iterator iterator = pinyinSet.iterator();//支持多音字
                    while (iterator.hasNext()) {//支持多音字
                        final String pinyin = iterator.next().toString().toLowerCase();//取出多音字里的一个字母

                        if (pinyin.indexOf(prefixString) != -1) {//任意匹配
                            newValues.add(value);
                        } else if (valueText.indexOf(prefixString) != -1) {//如果是汉字则直接添加
                            newValues.add(value);
                        }
                    }
                    if (maxMatch > 0) {//有数量限制
                        if (newValues.size() > maxMatch - 1) {//不要太多
                            break;
                        }
                    }

                }
                List<T> list = new ArrayList<T>(newValues);//转成List
                results.values = list;
                results.count = list.size();
            }
            return results;
        }

        protected void publishResults(CharSequence constraint, FilterResults results) {
            mObjects = (List<T>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

}  