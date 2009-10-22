package org.labkey.test.util;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Oct 22, 2009
 * Time: 11:01:23 AM
 */
public class Diff
{

    public static String diff(String a, String b)
    {
        return _diff(StringUtils.split(a), StringUtils.split(b));
    }


    static String _diff(String[] x, String[] y)
    {
        StringBuilder sb = new StringBuilder();

        // number of lines of each file
        int M = x.length;
        int N = y.length;

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M + 1][N + 1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M - 1; i >= 0; i--)
        {
            for (int j = N - 1; j >= 0; j--)
            {
                if (equals(x[i], y[j]))
                    opt[i][j] = opt[i + 1][j + 1] + 1;
                else
                    opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
            }
        }

        // recover LCS itself and print out non-matching lines to standard output
        int i, j;
        for (i=0, j=0 ; i < M && j < N ; )
        {
            if (x[i].equals(y[j]))
            {
                sb.append("        ").append(x[i]).append("\n");
                i++;
                j++;
            }
            else if (opt[i + 1][j] >= opt[i][j + 1])
                sb.append("<       ").append(x[i++]).append("\n");
            else
                sb.append(">       ").append(y[j++]).append("\n");
        }

        // dump out remainder of one string if the other is exhausted
        while (i < M || j < N)
        {
            if (i == M)
                sb.append(">       ").append(y[j++]).append("\n");
            else if (j == N)
                sb.append("<       ").append(x[i++]).append("\n");
        }
        return sb.toString();
    }

    static boolean equals(String a, String b)
    {
        return a.hashCode()==b.hashCode() && a.equals(b);
    }
}
